package com.bormannqds.apps.wjh.datafusiontool;

import com.bormannqds.apps.wjh.lib.configuration.ConfigurationResource;
import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;
import com.bormannqds.apps.wjh.lib.resources.marketdata.L1QuoteFiltering;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.*;
import com.bormannqds.apps.wjh.lib.referencedata.ReferenceDataResource;

import com.bormannqds.lib.bricks.gateway.AppStatusInterface;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.Bracket;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesInputResource;
import com.bormannqds.lib.utils.chrono.CalendarUtils;
import com.bormannqds.lib.utils.system.FileSystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class DataFusionTool {
	public static void printUsage() {
		System.err.println("Usage: java -Dlog4j.configurationFile=etc/log4j.xml -jar deploy/dft.jar <file name>");
		System.err.println("\tfrom the WhatJustHappened home directory as working directory");
		System.err.println("\twhere <file name> is the file name of an _ORDERS.csv file in the trading/ subdirectory.");
		System.err.println("\tThe file name needs to match the template <strategy>_<date in yyyy-MM-dd format>_ORDERS.csv");
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			printUsage();
			System.exit(1);
		}

		if (!args[0].endsWith("_ORDERS.csv")) {
			System.err.println(args[0] + " does not appear to be an ORDERS trading data file.");
			printUsage();
			System.exit(1);
		}

		ConfigurationResource configResource = null;
		boolean exit = true;
		Path cwd = Paths.get("");//.normalize();
//		Path cwd = Paths.get(System.getProperty("user.dir"));
		Path configFilePath = cwd.resolve("etc/WjhConfig.xml");
		URL configUrl = null;
		try {
			if (Files.notExists(configFilePath)) {
				BufferedWriter newConfigWriter = Files.newBufferedWriter(configFilePath, Charset.forName("UTF-8"), StandardOpenOption.CREATE_NEW);
				newConfigWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				newConfigWriter.newLine();
				newConfigWriter.write("<configuration/>");
				newConfigWriter.close();
			}

			if (!Files.isRegularFile(configFilePath)){
				LOGGER.fatal(configFilePath + " is not a regular file!");
			}
			else if (!(Files.isReadable(configFilePath)
					&& Files.isWritable(configFilePath))) {
				LOGGER.fatal(configFilePath + " is not read/writeable!");				
			}
			else {
				configUrl = configFilePath.toUri().toURL();
				configResource = new ConfigurationResource(configUrl);
				configResource.open();
				if (configResource.isOpen()) {
					exit = false;
				}
			}
		}
		catch(MalformedURLException mue) {
			LOGGER.fatal("BUG: Malformed URL to configuration file: " + configUrl, mue);
		}
		catch(IOException ioe) {
			LOGGER.fatal("Could not create/open/load configuration file: " + configFilePath, ioe);
		}

		if (exit) {
			System.exit(1);			
		}
		exit = true;

		URL outputLocator = configResource.getOutputBaseLocator();
		Path outputPath = null;
		try {
			outputPath = Paths.get(outputLocator.toURI());
			FileSystemUtils.createRWDirectory(outputPath);
			exit = false;
		}
		catch (URISyntaxException use) {
			LOGGER.fatal("Malformed output base locator: " + outputLocator, use);
		}
		catch (IOException ioe) {
			LOGGER.fatal("Could not create output directory: " + outputPath, ioe);
		}

		if (exit) {
			System.exit(1);			
		}

		DataFusionTool dataFusionTool = new DataFusionTool(cwd.toUri(), configResource);
		try {
			dataFusionTool.doSetup(args[0]);
			dataFusionTool.fuse();
			ApplicationContext.getInstance().getAppStatusBean().setStatus("Data fusion succeeded for " + args[0]);
			dataFusionTool.dispose();
		}
		catch (ResourceNotFoundException |ResourceIOException me) {
			// here we are!
			LOGGER.error("Problem opening/using/closing orders+associated resources!", me);
			ApplicationContext.getInstance().getAppStatusBean().setStatus("Problem opening/using/closing orders+associated resources!");
		}
		catch (IOException ioe) {
			LOGGER.error("Problem creating/writing PTA data files!", ioe);
			ApplicationContext.getInstance().getAppStatusBean().setStatus("Problem creating/writing PTA data files!");
		}
	}

	public DataFusionTool() {
		// default constructor for the GUI version; nothing to do
	}

	public DataFusionTool(URI appWorkingDirectoryUri, ConfigurationResource configResource) {
		isInStandaloneMode = true;

		URL refDataResourceLocator = configResource.getRefDataResourceLocator();
		ReferenceDataResource refDataResource;
		if (refDataResourceLocator == null) {
			refDataResource = new ReferenceDataResource();			
		}
		else {
			refDataResource = new ReferenceDataResource(refDataResourceLocator);
			try {
				refDataResource.open();
			} catch (ResourceIOException rioe) {
				LOGGER.fatal("Error whilst trying to load the Reference Data file! Please fix file or remove configuration...", rioe);
				System.exit(1);
			}
		}
		ApplicationContext.createInstance(Paths.get(appWorkingDirectoryUri), configResource, refDataResource);
		ApplicationContext.getInstance().setAppStatusBean(new AppStatusBean("Ready!"));
	}

	public ButtonGroup getLineSeparatorButtonGroup() {
		return lineSepButtonGroup;
	}

	public ButtonGroup getL1QuoteFilterButtonGroup() {
		return quoteFilterButtonGroup;
	}

	public void setup(final String ordersFilename) throws ResourceIOException, ResourceNotFoundException {
		final String lineSeparator = lineSepButtonGroup.getSelection().getActionCommand();
		if (lineSeparator != ApplicationContext.getInstance().getConfigurationResource().getDefaultCsvRecordSeparator()) {
			ApplicationContext.getInstance().getConfigurationResource().setDefaultCsvRecordSeparator(lineSeparator);
		}

		doSetup(ordersFilename); 
	}

	public void fuse() throws ResourceNotFoundException, IOException {
		Date ts0 = null;
		try {
			ts0 = tsFormat.parse(dateString + " 00:00:00.000");
		}
		catch (ParseException pe) {
			LOGGER.error("Date on orders file is not of the expected format!", pe);
			// we'll catch a resource error later...
		}

		TradingDataResource tdResource = ApplicationContext.getInstance().getTradingDataResource(strategy);

		// --- Process orders ---
		// Note that market data is taken care of by ParentOrderContext instances
		final TimeSeriesInputResource<Order> orderStream = tdResource.getOrderStream(date);
		orderStream.open();

		ParentOrderContext poContext = null;
		Bracket<Order> orderBracket = orderStream.getBracket(ts0);
		for (; orderBracket.getLater() != null; orderBracket = orderStream.propagateBracket(orderBracket)) {
			processOrderEvent(orderBracket.getEarlier());
		}
		processOrderEvent(orderBracket.getEarlier());
		orderStream.close();

		// --- Add fills to the mix ---
		final TimeSeriesInputResource<Fill> fillStream = tdResource.getFillStream(date);
		fillStream.open();

		Bracket<Fill> fillBracket = fillStream.getBracket(ts0);
		for (; fillBracket.getLater() != null; fillBracket = fillStream.propagateBracket(fillBracket)) {
			if (fillBracket.getEarlier() != null) { // not the start of the order stream
				poContext.processFillEvent(date, fillBracket.getEarlier());
			}
			poContext = poContexts.get(parentOrders.get(fillBracket.getLater().getParentOrderId()));
		}
		poContext.processFillEvent(date, fillBracket.getEarlier());
		fillStream.close();

		// --- Shake and Persist fused parent order data to data sink ---
		// and export booking data tags-to-parent order id map
		final URL poMappingLocator = new URL(strategyLocator, "parent_order_ids.csv");
		try {
			final BufferedWriter poMappingWriter = FileSystemUtils.createBufferedWriter(poMappingLocator);
			poMappingWriter.write("bookingtag,altid,parentorderid");
			poMappingWriter.newLine();
			for (final ParentOrderContext curPoContext: poContexts) {
				curPoContext.exportPtaData();
				final ParentOrder parentOrder = curPoContext.getParentOrder();
				final StringBuilder lineBuilder = new StringBuilder(parentOrder.getBookingData().getBookingTag());
				lineBuilder.append(',').append(parentOrder.getAltId()).append(',').append(parentOrder.getOrderId());
				poMappingWriter.write(lineBuilder.toString());
				poMappingWriter.newLine();
			}
			poMappingWriter.close();
		}
		catch (URISyntaxException use) {
			LOGGER.error("Malformed strategy parent order mapping file locator: " + poMappingLocator, use);
		}

		// export effective strategy legs
		final URL legsLocator = new URL(strategyLocator, "legs.csv");
		try {
			final BufferedWriter legsWriter = FileSystemUtils.createBufferedWriter(legsLocator);
			legsWriter.write("legtag,ticker");
			legsWriter.newLine();
			for (final Entry<LegTag, String> legEntry: strategyLegs.entrySet()) {
				final StringBuilder lineBuilder = new StringBuilder(legEntry.getKey().toString());
				lineBuilder.append(',').append(legEntry.getValue());
				legsWriter.write(lineBuilder.toString());
				legsWriter.newLine();
			}
			legsWriter.close();
		}
		catch (URISyntaxException use) {
			LOGGER.error("Malformed strategy legs mapping file locator: " + legsLocator, use);
		}
	}

	public void dispose() throws IOException {
		for (final ParentOrderContext poContext: poContexts) {
			poContext.dispose();
		}
		poContexts.clear();
		parentOrders.clear();
		strategyLegs.clear();
	}

	// -------- Private ----------

	private static class AppStatusBean implements AppStatusInterface {
		public AppStatusBean(final String initialStatus) {
			this.status = initialStatus;
		}

		@Override
		public String getStatus() {
			return status;
		}

		@Override
		public void setStatus(final String status) {
			this.status = status;
			LOGGER.info(status);
		}

		// -------- Private ----------

		private String status;
	}

	private void prepareStorage() throws ResourceIOException, ResourceNotFoundException {
		strategyLocator = null;

		final StringBuilder datePathnameBuilder = new StringBuilder(dateString);
		datePathnameBuilder.append('/');
		final StringBuilder strategyPathnameBuilder = new StringBuilder(strategy);
		strategyPathnameBuilder.append('/');
		// !!!!! IN THIS ORDER !!!!!
		URL dateLocator = null;
		Path datePath = null;
		try {
			dateLocator = new URL(ApplicationContext.getInstance().getConfigurationResource().getOutputBaseLocator(),
									datePathnameBuilder.toString());
			datePath = Paths.get(dateLocator.toURI());
			LOGGER.debug("date directory: " + datePath);
			FileSystemUtils.createRWDirectory(datePath);
		}
		catch (URISyntaxException use) {
			LOGGER.fatal("Malformed output or date base locator: " + dateLocator, use);
			throw new ResourceNotFoundException("Malformed output or date base locator: " + dateLocator, use);
		}
		catch (IOException ioe) {
			LOGGER.fatal("Could not create <date>/ subdirectory: " + datePath, ioe);
			throw new ResourceIOException("Could not create <date>/ subdirectory: " + datePath, ioe);
		}
		// let it crash&burn if Paths.get(..) fails

		Path strategyPath = null;
		try {
			strategyLocator = new URL(dateLocator, strategyPathnameBuilder.toString());
			strategyPath = Paths.get(strategyLocator.toURI());
			LOGGER.debug("strategy directory: " + strategyPath);
			FileSystemUtils.createRWDirectory(strategyPath);
		}
		catch (URISyntaxException use) {
			LOGGER.fatal("Malformed date or strategy base locator: " + strategyLocator, use);
			throw new ResourceNotFoundException("Malformed date or strategy base locator: " + strategyLocator, use);
		}
		catch (IOException ioe) {
			LOGGER.fatal("Could not create <date>/<strategy>/ subdirectory: " + strategyPath, ioe);
			throw new ResourceIOException("Could not create <date>/<strategy>/ subdirectory: " + strategyPath, ioe);
		}
		// let it crash&burn if Paths.get(..) fails
	}

	private void doSetup(final String ordersFilename) throws ResourceIOException, ResourceNotFoundException {
		final int strategyEndNdx = ordersFilename.length() - (1 + CalendarUtils.DATE_FORMAT.length() + DataAccessUtils.ORDERS_FILE_SUFFIX.length());
		final int dateEndNdx = ordersFilename.length() - DataAccessUtils.ORDERS_FILE_SUFFIX.length();
		strategy = ordersFilename.substring(0, strategyEndNdx);
		dateString = ordersFilename.substring(strategyEndNdx + 1, dateEndNdx);
		try {
			date = dateFormat.parse(dateString);
		}
		catch (ParseException pe) {
			LOGGER.error("Date on orders file is not of the expected format!", pe);
			// we'll catch a resource error later...
		}

		prepareStorage(); 
	}

	private L1QuoteFiltering determineQuoteFiltering() {
		return isInStandaloneMode ? ApplicationContext.getInstance().getConfigurationResource().getDefaultQuoteFiltering() : L1QuoteFiltering.valueOf(quoteFilterButtonGroup.getSelection().getActionCommand());
	}

	private ParentOrderContext switchParentOrderContextForOrder(final Order protoOrder) throws IOException, ResourceNotFoundException {
		Integer poContextNdx = parentOrders.get(protoOrder.getParentOrderId());
		if (poContextNdx == null) {
			ParentOrderContext poContext = new ParentOrderContext(strategyLocator, protoOrder, determineQuoteFiltering());
			poContexts.add(poContext);
			parentOrders.put(protoOrder.getParentOrderId(), poContexts.size() - 1);
			LOGGER.info("Started processing parent order " + protoOrder.getParentOrderId());
			return poContext;
		}

		return poContexts.get(poContextNdx);
	}

	private void processOrderEvent(final Order order) throws IOException, ResourceNotFoundException {
		if (order != null) {
			final ParentOrderContext poContext = switchParentOrderContextForOrder(order);
			poContext.processOrderEvent(date, order);
			if (!strategyLegs.containsKey(order.getLegTag())) {
				strategyLegs.put(order.getLegTag(), order.getTicker());
			}
		}
	}

	private static final Logger LOGGER = LogManager.getLogger(DataFusionTool.class);

	private final DateFormat dateFormat = new SimpleDateFormat(CalendarUtils.DATE_FORMAT); // let's assume for now we're single-threaded
//	private final DateFormat kdbDateFormat = new SimpleDateFormat(DatePatterns.KDB_DATE_FORMAT); // let's assume for now we're single-threaded
	private final DateFormat tsFormat = new SimpleDateFormat(CalendarUtils.TIMESTAMP_FORMAT); // let's assume for now we're single-threaded
	private final List<ParentOrderContext> poContexts = new ArrayList<ParentOrderContext>(); // needed to maintain input order rather than use a TreeMap with String key type.
	private final Map<String, Integer> parentOrders = new HashMap<String, Integer>();
	private final Map<LegTag, String> strategyLegs = new TreeMap<LegTag, String>();

	private final ButtonGroup lineSepButtonGroup = new ButtonGroup();
	private final ButtonGroup quoteFilterButtonGroup = new ButtonGroup();

	private boolean isInStandaloneMode = false;
	private String strategy;
	private String dateString;
	private Date date;
	private URL strategyLocator;
}

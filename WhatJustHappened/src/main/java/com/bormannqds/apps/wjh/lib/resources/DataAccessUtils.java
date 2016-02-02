package com.bormannqds.apps.wjh.lib.resources;

import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.utils.chrono.CalendarUtils;
import com.bormannqds.lib.utils.system.OsCheck;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DataAccessUtils {
	public static final String ORDERS_FILE_SUFFIX = "_ORDERS.csv";

	public static String createRelativePathname(final String dateString, final String strategy) {
		StringBuilder relPathBuilder = new StringBuilder(dateString);
		relPathBuilder.append('/');
		relPathBuilder.append(strategy);
		relPathBuilder.append('/');

		return relPathBuilder.toString();
	}

	public static URL appendPtaBaseLocator(final URL baseLocator, final String dateString, final String strategy) throws ResourceNotFoundException {
		try {
			return new URL(baseLocator, createRelativePathname(dateString, strategy));
		}
		catch (MalformedURLException mue) {
			LOGGER.fatal("Data base path locator is malformed: " + baseLocator, mue);
			throw new ResourceNotFoundException("Data base path locator is malformed: " + baseLocator, mue);
		}		
	}

	public static URL createQuotesLocator(final URL baseLocator, final String ticker, final Date date) throws ResourceNotFoundException {
		return createLocator(baseLocator, ticker, date, L1BOOK_FILE_SUFFIX);
	}

	public static URL createTradesLocator(final URL baseLocator, final String ticker, final Date date) throws ResourceNotFoundException {
		return createLocator(baseLocator, ticker, date, TRADES_FILE_SUFFIX);
	}

	public static URL createOrdersLocator(final URL baseLocator, final String strategy, final Date date) throws ResourceNotFoundException {
		return createLocator(baseLocator, strategy, date, ORDERS_FILE_SUFFIX);
	}

	public static URL createFillsLocator(final URL baseLocator, final String strategy, final Date date) throws ResourceNotFoundException {
		return createLocator(baseLocator, strategy, date, FILLS_FILE_SUFFIX);
	}

	public static URL createPtaLocator(final URL baseLocator, final String id) throws ResourceNotFoundException {
		String escapedId = null;
		switch (OsCheck.getOperatingSystemType()) {
		case WINDOWS:
		case OTHER:
			escapedId = id.replaceAll(":", "-").replaceAll(" ", getRfc2396EscSeq(' '));
			break;
		case LINUX:
		case MACOS:
			escapedId = id.replaceAll(":", getRfc2396EscSeq(':')).replaceAll(" ", getRfc2396EscSeq(' '));
			break;
		}
		return createLocator(baseLocator, escapedId, null, PTA_FILE_SUFFIX);
	}

	// -------- Private ----------

	private static URL createLocator(final URL baseLocator, final String id, final Date date, final String locatorSuffix) throws ResourceNotFoundException {
		StringBuilder extBuilder = new StringBuilder(id);
		if (date != null) {
			DateFormat dateFormat = new SimpleDateFormat(CalendarUtils.DATE_FORMAT);
			String dateString = dateFormat.format(date);
			extBuilder.append('_');
			extBuilder.append(dateString);
		}
		extBuilder.append(locatorSuffix);

		try {
			return new URL(baseLocator, extBuilder.toString());
		}
		catch (MalformedURLException mue) {
			LOGGER.fatal("Data base path locator is malformed: " + baseLocator, mue);
			throw new ResourceNotFoundException("Data base path locator is malformed: " + baseLocator, mue);
		}
	}

	private static String getRfc2396EscSeq(char c) {
		final StringBuilder escSeqBuilder = new StringBuilder("%");
		escSeqBuilder.append(Integer.toHexString(c));

		return escSeqBuilder.toString();
	}

	private static final Logger LOGGER = LogManager.getLogger(DataAccessUtils.class);
	private static final String L1BOOK_FILE_SUFFIX = "_L1BOOK.csv";
	private static final String TRADES_FILE_SUFFIX = "_TRADE.csv";
	private static final String FILLS_FILE_SUFFIX = "_FILLS.csv";
	private static final String PTA_FILE_SUFFIX = "_PTA.csv";
}

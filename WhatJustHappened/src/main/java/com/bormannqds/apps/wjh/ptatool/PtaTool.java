package com.bormannqds.apps.wjh.ptatool;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.AltId;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.LegTag;

import com.bormannqds.lib.dataaccess.timeseries.Filter;
import com.bormannqds.lib.utils.chrono.CalendarUtils;
import com.bormannqds.lib.utils.system.FileSystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

public class PtaTool {
	public TreeModel getPtaDataTreeModel() {
		return ptaDataTreeModel;
	}

	public SpinnerModel getCutoffDateModel() {
		return spinnerModel;
	}

	public Map<LegTag, String> getBasketLegMap() {
		return basketLegMap;
	}

	public TableModel getBasketModel() {
		return basketTableModel;
	}

	public ComboBoxModel<String> getBookingIdModel() {
		return bookingIdModel;
	}

	public ComboBoxModel<String> getParentOrderIdModel() {
		return poIdModel;
	}

	public PtaDataDescriptor getSelectedPtaDataDescriptor() {
		ptaDataDescriptor.setParentOrderId((String)poIdModel.getSelectedItem());
		return ptaDataDescriptor;
	}

	public void setTradingPhaseModel(AltId tradingPhase, ButtonModel model) {
		tradingPhaseModelMap.put(tradingPhase, model);
	}

	/**
	 * Target of initial and reload population of the PTA Data selection tree with the sub-directory structure
	 * of the trading data directory.
	 * 
	 * Constraints: cut-off date, obtained from SpinnerDateModel of the "Cut-off Date" spinner
	 */
	public boolean buildPtaDataTreeModel() {
		rootNode.removeAllChildren();

		URL outputLocator = ApplicationContext.getInstance().getConfigurationResource().getOutputBaseLocator();
		try {
			outputPath = Paths.get(outputLocator.toURI());
			final Date cutoffDate = (Date)spinnerModel.getValue();
			if (FileSystemUtils.isExistingReadableDirectory(outputPath)) {
				DirectoryStream<Path> dateFilteredSubdirEntries = FileSystemUtils.getDateNameFilteredSubdirDirectoryStream(outputPath, cutoffDate);
				for (Iterator<Path> dateNamedSubdirPathIterator = dateFilteredSubdirEntries.iterator(); dateNamedSubdirPathIterator.hasNext();) {
					Path dateNamedSubdirPath = dateNamedSubdirPathIterator.next();
					DefaultMutableTreeNode branchNode = new DefaultMutableTreeNode(dateNamedSubdirPath.getFileName());
					DirectoryStream<Path> strategySubdirEntries = FileSystemUtils.getSubdirFilteredDirectoryStream(dateNamedSubdirPath);
					for (Iterator<Path> strategySubdirPathIterator = strategySubdirEntries.iterator(); strategySubdirPathIterator.hasNext();) {
						branchNode.add(new DefaultMutableTreeNode(strategySubdirPathIterator.next().getFileName(), false));
					}
					rootNode.add(branchNode);
				}

				if (ptaDataTreeModel == null) {
					ptaDataTreeModel = new DefaultTreeModel(rootNode, true);
				}
				else {
					ptaDataTreeModel.reload(rootNode);
				}

				return true;
			}
		}
		catch (URISyntaxException use) {
			LOGGER.error("BUG: Should not happen at this stage: Malformed output base locator: " + outputLocator, use);
			return false;
		}
		catch (IOException ioe) {
			LOGGER.error("Problem reading PTA directory structure at " + outputPath, ioe);
			return false;
		}

		LOGGER.error("Could not find/read output directory: " + outputPath
					+ ". It still is a directory, right? Has the rug been pulled from under our feet?");
		return false;
	}

	public void buildPoIdModel() {
		if (poIdModel.getSize() > 0)
			poIdModel.removeAllElements();

		if (parentOrderRawDataMap.isEmpty()) return; // nothing to do

		List<String> filteredPoIdList = filterParentOrderIds(parentOrderRawDataMap.keySet());
		poIdModel.addElement(UNIVERSAL_SELECTOR);
		for (String parentOrderId: filteredPoIdList) {
			poIdModel.addElement(parentOrderId);
		}
		poIdModel.setSelectedItem(filteredPoIdList.get(0));
	}

	/**
	 * Target of PTA Data selection tree leaf node selection action.
	 * 
	 * @param dateSubdirPath
	 * @param strategySubdirPath
	 */
	public void populatePtaDataSelectionModels(final Path dateSubdirPath, final Path strategySubdirPath) {
		ptaDataDescriptor.setDateString(dateSubdirPath.getFileName().toString());
		ptaDataDescriptor.setStrategy(strategySubdirPath.getFileName().toString());
		final Path dataPath = outputPath.resolve(dateSubdirPath).resolve(strategySubdirPath);
		LOGGER.debug("Path to selected PTA data: " + dataPath);
		basketLegMap.clear();
		createBasketLegMapFromLegsFile(dataPath);
		prepareBasketModel();
		createRawDataMapFromParentOrderFile(dataPath);
		// in this order because of the filtering!
		buildBookingIdModel();
		buildPoIdModel();
	}

	/**
	 * Target of the Reload button action.
	 */
	public void resetPtaDataSelectionModels() {
		ptaDataDescriptor.reset();
		basketLegMap.clear();
		resetTradingPhaseModels();
		prepareBasketModel();
		buildBookingIdModel();
		buildPoIdModel();
	}

	// -------- Private ----------

	@SuppressWarnings("serial")
	private static class BasketTableModel extends DefaultTableModel {
		public BasketTableModel(Object[][] data, String[] columnNames) {
			super(data, columnNames);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnTypes[columnIndex];
		}
		@Override
		public boolean isCellEditable(int row, int column) {
			return columnEditables[column];
		}

		// -------- Private ----------

		private static final Class<?>[] columnTypes = new Class[] { Object.class, String.class };
		private final boolean[] columnEditables = new boolean[] { false, false };
	}

	private class SummaryParentOrder {
		public SummaryParentOrder(String bookingId, AltId altId) {
			this.bookingId = bookingId;
			this.altId = altId;
		}

		public String getBookingId() {
			return bookingId;
		}

		public AltId getAltId() {
			return altId;
		}

		// -------- Private ----------

		private final String bookingId;
		private final AltId altId;
	}

	/**
	 * Parent order filter that uses the booking id and trading phase models to figure out filter settings
	 */
	private class ParentOrderIdFilter implements Filter<String> {
		@Override
		public boolean accept(String input) {
			SummaryParentOrder spo = parentOrderRawDataMap.get(input);
			if (spo == null) return false; // should never happen!

			ButtonModel tradingPhaseModel = tradingPhaseModelMap.get(spo.getAltId());
			if ((bookingIdModel.getSelectedItem().equals(UNIVERSAL_SELECTOR)
					|| bookingIdModel.getSelectedItem().equals(spo.getBookingId()))
				&& tradingPhaseModel != null && tradingPhaseModel.isSelected()) {
				return true;
			}

			return false;
		}
	}

	private void createBasketLegMapFromLegsFile(final Path dataPath) {
		try {
			BufferedReader legsReader = FileSystemUtils.createBufferedReader(dataPath.resolve("legs.csv"));
			if (legsReader == null) {
				ApplicationContext.getInstance().getAppStatusBean().setStatus("No legs.csv file found. Please regenerate data using the Data Fusion tool.");
				return;
			}
			for (String curLine = legsReader.readLine();
				 curLine != null && basketLegMap.size() < LegTag.MAX_NR_LEGS.ordinal();
				 curLine = legsReader.readLine()) {
				if (curLine.contains("legtag,ticker")) { // skip header
					continue;
				}
				String[] tokens = curLine.split(",");
				if (tokens == null || tokens.length != 2) {
					break;
				}
				try {
					LegTag legTag = LegTag.valueOf(tokens[0]);
					basketLegMap.put(legTag, tokens[1]);
				}
				catch (IllegalArgumentException iae) {
					break;
				}
			}
		}
		catch (IOException ioe) {
			LOGGER.error("Cannot read strategy legs.csv file in " + dataPath, ioe);
		}
	}

	private void createRawDataMapFromParentOrderFile(final Path dataPath) {
		parentOrderRawDataMap.clear();
		try {
			BufferedReader poReader = FileSystemUtils.createBufferedReader(dataPath.resolve("parent_order_ids.csv"));
			if (poReader == null) {
				ApplicationContext.getInstance().getAppStatusBean().setStatus("No parent_order_ids.csv file found. Please regenerate data using the Data Fusion tool.");
				return;
			}
			for (String curLine = poReader.readLine(); curLine != null; curLine = poReader.readLine()) {
				if (curLine.contains("bookingtag,altid,parentorderid")) { // skip header
					continue;
				}
				String[] tokens = curLine.split(",");
				if (tokens == null || tokens.length != 3) {
					LOGGER.error("Rubbish line in the strategy parent_order_ids.csv file: " + curLine);
					break; // rubbish in the file, stop processing
				}
				try {
					AltId altId = AltId.valueOf(tokens[1]);
					parentOrderRawDataMap.put(tokens[2], new SummaryParentOrder(tokens[0], altId));
				}
				catch (IllegalArgumentException iae) {
					LOGGER.error("Rubbish AltId field in the strategy parent_order_ids.csv file: " + tokens[1], iae);
					break;
				}
			}
		}
		catch (IOException ioe) {
			LOGGER.error("Cannot read strategy parent_order_ids.csv file in " + dataPath, ioe);
		}
	}

	private void resetTradingPhaseModels() {
		for (Entry<AltId, ButtonModel> tradingPhaseMapping: tradingPhaseModelMap.entrySet()) {
			boolean shouldBeSelected;
			switch (tradingPhaseMapping.getKey()) {
			case Q:
			case FT:
				shouldBeSelected = true;
				break;
			case TSB:
			case TSS:
			default:
				shouldBeSelected = false;
			}
			tradingPhaseMapping.getValue().setArmed(shouldBeSelected);
			tradingPhaseMapping.getValue().setSelected(shouldBeSelected);
		}
	}

	private void prepareBasketModel() {
		// ---- this is not efficiently implemented garbage-wise but it allows to keep the tree selection listener local ----
		// ---- fortunately, quantities are low and only driven by infrequent user interaction                                       ----
		int excessRows = basketLegMap.isEmpty() ? 1 : basketLegMap.size();
		for (int rowNdx = basketTableModel.getRowCount() - 1; rowNdx >= excessRows; --rowNdx) {
			basketTableModel.removeRow(rowNdx);
		}
		if (basketLegMap.isEmpty()) {
			basketTableModel.setValueAt(DEFAULT_TICKER_CELLVALUE, LegTag.L1.ordinal(), 1);
		}
		else {
			for (Entry<LegTag, String> mapping: basketLegMap.entrySet()) {
				if (mapping.getKey().ordinal() < basketTableModel.getRowCount()) {
					basketTableModel.setValueAt(mapping.getValue(), mapping.getKey().ordinal(), 1);
				}
				else {
					basketTableModel.addRow(new Object[] { mapping.getKey(), mapping.getValue() });
				}
			}
		}
	}

	private void buildBookingIdModel() {
		bookingIdModel.removeAllElements();
		if (parentOrderRawDataMap.isEmpty()) return; // nothing to do

		Set<String> bookingIdSet = new HashSet<String>(); 
		for (SummaryParentOrder spo: parentOrderRawDataMap.values()) {
			bookingIdSet.add(spo.getBookingId());
		}
		List<String> bookingIds = new ArrayList<String>(bookingIdSet);
		Collections.sort(bookingIds);
		bookingIdModel.addElement(UNIVERSAL_SELECTOR);
		for (String bookingId: bookingIds) {
			bookingIdModel.addElement(bookingId);
		}
		bookingIdModel.setSelectedItem(bookingIds.get(0));
	}

	private List<String> filterParentOrderIds(final Set<String> srcPoList) {
		List<String> dstPoList = new ArrayList<String>();
		for (String parentOrderId: srcPoList) {
			if (poFilter.accept(parentOrderId)) {
				dstPoList.add(parentOrderId);
			}
		}
		Collections.sort(dstPoList);
		return dstPoList;
	}

	private static final Logger LOGGER = LogManager.getLogger(PtaTool.class);
	private static final String DEFAULT_TICKER_CELLVALUE = "<ticker>";
	private static final String UNIVERSAL_SELECTOR = "ALL";
	private static final String[] COLUMN_HEADERS = new String[] { "Leg", "Ticker"};
	private static final Date TODAY = CalendarUtils.toStartOfDay(new Date());
	private static final Date FORTHNIGHT = CalendarUtils.nrWeeksEarlier(TODAY, 2);

	private final Map<String, SummaryParentOrder> parentOrderRawDataMap = new HashMap<String, SummaryParentOrder>();
	private final ParentOrderIdFilter poFilter = new ParentOrderIdFilter();
	private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("PTA Data");
	private final SpinnerModel spinnerModel = new SpinnerDateModel(FORTHNIGHT, null, TODAY, Calendar.MONTH);
	private final Map<AltId, ButtonModel> tradingPhaseModelMap = new TreeMap<AltId, ButtonModel>();
	private final Map<LegTag, String> basketLegMap = new TreeMap<LegTag, String>();
	private final BasketTableModel basketTableModel = new BasketTableModel(new Object[][] { { LegTag.L1, DEFAULT_TICKER_CELLVALUE }, }, COLUMN_HEADERS);
	private final DefaultComboBoxModel<String> bookingIdModel = new DefaultComboBoxModel<String>();
	private final DefaultComboBoxModel<String> poIdModel = new DefaultComboBoxModel<String>();
	private final PtaDataDescriptor ptaDataDescriptor = new PtaDataDescriptor();
	private Path outputPath = null;
	private DefaultTreeModel ptaDataTreeModel = null;
}

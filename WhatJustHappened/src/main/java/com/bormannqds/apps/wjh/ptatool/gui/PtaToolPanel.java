package com.bormannqds.apps.wjh.ptatool.gui;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.AltId;
import com.bormannqds.apps.wjh.ptatool.PtaTool;

import com.bormannqds.lib.bricks.gui.AbstractPanel;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.utils.chrono.CalendarUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * To Dos:
 * 	DONE a) complete implementation of tree model builder from directory data
 *  DONE b) implement "Reload" button action, using functionality under a)
 *  DONE c) implement tree leaf node selection listener:
 *	DONE	  (i) read corresponding PTA meta data from disk
 *	DONE	 (ii) populate centre panel with read meta data
 *	DONE d) implement "Generate charts..." button action to ... generate charts :-)
 *		  (i) read PTA data from disk for selected parent order set
 *		 (ii) per leg: filter and massage data into JFreeChart data models
 *		(iii) per leg: create chart object and feed with data models created under (ii)
 *
 * @author guy
 *
 */
@SuppressWarnings("serial")
public class PtaToolPanel extends AbstractPanel {
	/**
	 * Create the panel with a BorderLayout.
	 */
	public PtaToolPanel() {
		super(new BorderLayout(0, 0));
		isPtaDataTreeModelReady = ptaTool.buildPtaDataTreeModel();
		initialise();
		if (!isPtaDataTreeModelReady) {
			btnReloadButton.setEnabled(false);
		}
		btnGenerateChartsButton.setEnabled(false);
	}

	public boolean isPtaDataTreeModelReady() {
		return isPtaDataTreeModelReady;
	}

	public void dispose() {
		ptaChartDialog.dispose();
	}
	// -------- Private ----------

	private class ReloadAction extends AbstractAction {

		public ReloadAction(final Object caller, final List<Action> tradingPhaseActions, final Action bookingIdAction, Action generateChartsAction) {
			putValue(ACTION_COMMAND_KEY, "ReloadPTADataSelectionTree");
			putValue(NAME, "Reload");
			putValue(SHORT_DESCRIPTION, "Reload PTA Data selection tree from disk...");
			this.caller = caller;
			this.tradingPhaseActions = tradingPhaseActions;
			this.bookingIdAction = bookingIdAction;
			this.generateChartsAction = generateChartsAction;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == caller) {
				ptaDataTree.clearSelection();
				generateChartsAction.setEnabled(false);
				bookingIdAction.setEnabled(false);
				for (Action action: tradingPhaseActions) {
					action.setEnabled(false);
				}
				isPtaDataTreeModelReady = ptaTool.buildPtaDataTreeModel();
				ptaTool.resetPtaDataSelectionModels();
				bookingIdAction.setEnabled(true);
				for (Action action: tradingPhaseActions) {
					action.setEnabled(true);
				}
			}
		}

		// -------- Private ----------

		private Object caller;
		private List<Action> tradingPhaseActions;
		private Action bookingIdAction;
		private Action generateChartsAction;
	}

	private class BookingIdAction extends AbstractAction {
		public BookingIdAction(final Object caller) {
			putValue(ACTION_COMMAND_KEY, "BookingIdSelection");
			putValue(NAME, "Booking IDs");
			putValue(SHORT_DESCRIPTION, "Booking ID selection");
			this.caller = caller;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == caller && isEnabled()) { // TODO remove isEnabled() check once Action bug is fixed
				ptaTool.buildPoIdModel();
			}
		}

		private Object caller;		
	}

	private class TradingPhaseAction extends AbstractAction {
		public TradingPhaseAction(final Object caller, final AltId altId) {
			putValue(ACTION_COMMAND_KEY, "TradingPhaseSelection");
			putValue(NAME, altId.toString());
			putValue(SHORT_DESCRIPTION, "Trading phase selection");
			this.caller = caller;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == caller && isEnabled()) { // TODO remove isEnabled() check once Action bug is fixed
				ptaTool.buildPoIdModel();
			}
		}

		private Object caller;
	}

	private class GenerateChartsAction extends AbstractAction {
		public GenerateChartsAction(final Object caller) {
			putValue(ACTION_COMMAND_KEY, "ShowPtaChartsDialog");
			putValue(NAME, "Generate charts...");
			putValue(SHORT_DESCRIPTION, "Show the dialog with the generated PTA charts");
			this.caller = caller;
		}

		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == caller) {
				ApplicationContext.getInstance().getAppStatusBean().setStatus("PTA tool: generating charts...");
				try {
					ptaChartDialog.getPtaModel().createChartModels(ptaTool.getSelectedPtaDataDescriptor());
				}
				catch (ResourceIOException | ResourceNotFoundException mre) {
					LOGGER.error("Problem reading selected PTA data.", mre);
					ApplicationContext.getInstance().getAppStatusBean().setStatus("Problem reading selected PTA data (see log).");
					return; // don't show dialog
				}
				ptaChartDialog.prepareChartPanel(ptaTool.getSelectedPtaDataDescriptor().getParentOrderId(), ptaTool.getBasketLegMap());
				ptaChartDialog.setVisible(true);
			}
		}

		private Object caller;
	}

	private class StrategySelectionListener implements TreeSelectionListener {
		public StrategySelectionListener(final Object caller, final List<Action> tradingPhaseActions, final Action bookingIdAction, Action generateChartsAction) {
			this.caller = caller;
			this.bookingIdAction = bookingIdAction;
			this.tradingPhaseActions = tradingPhaseActions;
			this.generateChartsAction = generateChartsAction;
		}

		@Override
		public void valueChanged(TreeSelectionEvent tse) {
			if (tse.getSource() == caller) {
				generateChartsAction.setEnabled(false);

				TreePath selectedPath = tse.getPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectedPath.getLastPathComponent();
				
				if (node == null || !node.isLeaf()) {
					return;
				}

				Path strategySubdirPath = (Path)node.getUserObject();
				if (strategySubdirPath == null) {
					return;
				}

				TreePath parentPath = selectedPath.getParentPath();
				node = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
				
				if (node == null) {
					return;
				}

				Path dateSubdirPath = (Path)node.getUserObject();
				if (dateSubdirPath == null) {
					return;
				}

				ApplicationContext.getInstance().getAppStatusBean().setStatus("Selected PTA data for strategy " + strategySubdirPath
																				+ " from " + dateSubdirPath);

				bookingIdAction.setEnabled(false); // disarm to prevent parent order model activation on model-rebuilding
				for (Action action: tradingPhaseActions) {
					action.setEnabled(false);
				}
				ptaTool.populatePtaDataSelectionModels(dateSubdirPath, strategySubdirPath);
				bookingIdAction.setEnabled(true); // re-arm
				for (Action action: tradingPhaseActions) {
					action.setEnabled(true);
				}
				generateChartsAction.setEnabled(true); // arm for chart generation
			}
		}

		// -------- Private ----------

		private Object caller;
		private Action bookingIdAction;
		private List<Action> tradingPhaseActions;
		private Action generateChartsAction;
	}

	private void initialise() {
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		cutoffDateSpinner.setMaximumSize(new Dimension(95, 32767));
		cutoffDateSpinner.setModel(ptaTool.getCutoffDateModel());
		cutoffDateSpinner.setEditor(new JSpinner.DateEditor(cutoffDateSpinner, CalendarUtils.DATE_FORMAT));

		btnReloadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnReloadButton.setToolTipText("Reload PTA Data selection tree...");
		btnReloadButton.setHorizontalAlignment(SwingConstants.CENTER);

		ptaDataTree.setModel(ptaTool.getPtaDataTreeModel());
		ptaDataTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		basketTable.setToolTipText("Strategy basket");
		basketTable.setModel(ptaTool.getBasketModel());
		basketTable.getColumnModel().getColumn(0).setResizable(false);
		basketTable.getColumnModel().getColumn(1).setResizable(false);
		basketTable.getColumnModel().getColumn(0).setMaxWidth(35);
		basketTable.getColumnModel().getColumn(1).setMaxWidth(70);
		basketTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		basketTable.getTableHeader().setReorderingAllowed(false);
		basketTable.getTableHeader().setAlignmentX(CENTER_ALIGNMENT);

		bookingIdComboBox.setModel(ptaTool.getBookingIdModel());
		parentOrderIdComboBox.setModel(ptaTool.getParentOrderIdModel());

		btnGenerateChartsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnGenerateChartsButton.setToolTipText("Generate charts of selected PTA data...");
		btnGenerateChartsButton.setHorizontalAlignment(SwingConstants.CENTER);

		{
			Box northHBox = Box.createHorizontalBox();
			add(northHBox, BorderLayout.NORTH);
			northHBox.add(btnReloadButton);
			{
				Component horizontalGlue = Box.createHorizontalGlue();
				northHBox.add(horizontalGlue);
			}
			{
				JLabel lblNewLabel = new JLabel("Cut-off date:");
				northHBox.add(lblNewLabel);
			}
			northHBox.add(cutoffDateSpinner);
		}
		{
			JPanel eastPanel = new JPanel();
			add(eastPanel, BorderLayout.EAST);
			GroupLayout groupLayout = new GroupLayout(eastPanel);
			eastPanel.setLayout(groupLayout);
			eastPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
			{
				JLabel lblTradingPhases = new JLabel("Trading phases:");
				JCheckBox qCheckbox = new JCheckBox();
				qCheckbox.setSelected(true);
				JCheckBox ftCheckbox = new JCheckBox();
				ftCheckbox.setSelected(true);
				JCheckBox tsbCheckbox = new JCheckBox();
				JCheckBox tssCheckbox = new JCheckBox();

				qCheckbox.setAction(new TradingPhaseAction(qCheckbox, AltId.Q));
				ftCheckbox.setAction(new TradingPhaseAction(ftCheckbox, AltId.FT));
				tsbCheckbox.setAction(new TradingPhaseAction(tsbCheckbox, AltId.TSB));
				tssCheckbox.setAction(new TradingPhaseAction(tssCheckbox, AltId.TSS));

				tradingPhaseActions.add(qCheckbox.getAction());
				tradingPhaseActions.add(ftCheckbox.getAction());
				tradingPhaseActions.add(tsbCheckbox.getAction());
				tradingPhaseActions.add(tssCheckbox.getAction());

				groupLayout.setHorizontalGroup(
					groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap().addComponent(lblTradingPhases).addContainerGap())
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(qCheckbox))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(ftCheckbox))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(tsbCheckbox))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(tssCheckbox))
				);
				groupLayout.setVerticalGroup(
					groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap().addComponent(lblTradingPhases)
							.addGap(5).addComponent(qCheckbox, 20, 20, 20)
							.addGap(3).addComponent(ftCheckbox, 20, 20, 20)
							.addGap(3).addComponent(tsbCheckbox, 20, 20, 20)
							.addGap(3).addComponent(tssCheckbox, 20, 20, 20)
							.addContainerGap())
				);

				ptaTool.setTradingPhaseModel(AltId.Q, qCheckbox.getModel());
				ptaTool.setTradingPhaseModel(AltId.FT, ftCheckbox.getModel());
				ptaTool.setTradingPhaseModel(AltId.TSB, tsbCheckbox.getModel());
				ptaTool.setTradingPhaseModel(AltId.TSS, tssCheckbox.getModel());
			}
		}
		{
			Box southHBox = Box.createHorizontalBox();
			add(southHBox, BorderLayout.SOUTH);
			{
				southHBox.add(Box.createHorizontalGlue());
				southHBox.add(btnGenerateChartsButton);
				southHBox.add(Box.createHorizontalGlue());
			}
		}
		{
			JPanel westPanel = new JPanel();
			westPanel.setMinimumSize(new Dimension(130, 100));
			westPanel.setPreferredSize(new Dimension(130, 120));
			add(westPanel, BorderLayout.WEST);
			GroupLayout groupLayout = new GroupLayout(westPanel);
			westPanel.setLayout(groupLayout);
			{
				JScrollPane scrollPane = new JScrollPane(ptaDataTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				groupLayout.setHorizontalGroup(
						groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createSequentialGroup().addGap(5).addComponent(scrollPane))
				);
				groupLayout.setVerticalGroup(
					groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
				);
			}
		}
		{
			JPanel centrePanel = new JPanel();
			add(centrePanel, BorderLayout.CENTER);
			GroupLayout groupLayout = new GroupLayout(centrePanel);
			centrePanel.setLayout(groupLayout);
			{
				JLabel lblStrategyLegs = new JLabel("Strategy basket:");
				JLabel lblBookingIds = new JLabel("Booking ids:");
				JLabel lblParentOrders = new JLabel("Parent Orders:");

				groupLayout.setHorizontalGroup(
					groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(lblStrategyLegs))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(basketTable.getTableHeader()))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(basketTable))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(lblBookingIds))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(bookingIdComboBox, 90, 180, 180))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(lblParentOrders))
						.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(parentOrderIdComboBox, 200, 430, 450).addContainerGap())
					);
				groupLayout.setVerticalGroup(
					groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap().addComponent(lblStrategyLegs)
							.addGap(3).addComponent(basketTable.getTableHeader(), 25, 25, 25)
							.addComponent(basketTable)
							.addGap(30).addComponent(lblBookingIds)
							.addGap(3).addComponent(bookingIdComboBox, 25, 25, 25)
							.addGap(7).addComponent(lblParentOrders)
							.addGap(3).addComponent(parentOrderIdComboBox, 25, 25, 25)
							.addContainerGap())
				);
			}
		}

		// non-trivial action coupling
		Action bookingIdAction = new BookingIdAction(bookingIdComboBox);
		bookingIdComboBox.setAction(bookingIdAction);
		Action generateChartsAction = new GenerateChartsAction(btnGenerateChartsButton);
		btnGenerateChartsButton.setAction(generateChartsAction);
		btnReloadButton.setAction(new ReloadAction(btnReloadButton, tradingPhaseActions, bookingIdAction, generateChartsAction));
		ptaDataTree.addTreeSelectionListener(new StrategySelectionListener(ptaDataTree, tradingPhaseActions, bookingIdAction, generateChartsAction));
	}

	private static final Logger LOGGER = LogManager.getLogger(PtaToolPanel.class);

	private final PtaTool ptaTool = new PtaTool();
	private final JSpinner cutoffDateSpinner = new JSpinner();
	private final JButton btnReloadButton = new JButton("Reload");
	private final JTable basketTable = new JTable();
	private final JComboBox<String> bookingIdComboBox = new JComboBox<String>();
	private final JComboBox<String> parentOrderIdComboBox = new JComboBox<String>();
	private final List<Action> tradingPhaseActions = new ArrayList<Action>();
	private final JButton btnGenerateChartsButton = new JButton("Generate charts...");
	private final JTree ptaDataTree = new JTree();
	private final PtaChartDialog ptaChartDialog = new PtaChartDialog();
	private boolean isPtaDataTreeModelReady = false;
}

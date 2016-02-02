package com.bormannqds.apps.wjh.datafusiontool.gui;

import com.bormannqds.apps.wjh.datafusiontool.DataFusionTool;
import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;
import com.bormannqds.apps.wjh.lib.resources.marketdata.L1QuoteFiltering;

import com.bormannqds.lib.bricks.gui.AbstractPanel;
import com.bormannqds.lib.bricks.gui.AbstractShowOpenFileDialogAction;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.utils.system.FileSystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

@SuppressWarnings("serial")
public class DataFusionToolPanel extends AbstractPanel {

	/**
	 * Create the panel with a BorderLayout.
	 * @param fileChooser 
	 */
	public DataFusionToolPanel(final JFileChooser fileChooser) {
		super(new BorderLayout(0, 0));
		try {
			final URL tradingDataBaseLocator = ApplicationContext.getInstance().getConfigurationResource().getTrdDataResourceBaseLocator();
			if (tradingDataBaseLocator != null) {
				tradingDataDirectory = FileSystemUtils.toFile(tradingDataBaseLocator);
			}
		}
		catch (URISyntaxException use) {
			LOGGER.warn("Base path URI to trading data is malformed! Please check configuration file. ", use);
		}
		if (!isTradingDataDirectoryReady()) {
			btnSelectFile.setEnabled(false);
		}
		initialise(fileChooser);
	}

	public boolean isTradingDataDirectoryReady() {
		return tradingDataDirectory != null;
	}

	// -------- Private ----------

	private class ShowTradingDataDialogAction extends AbstractShowOpenFileDialogAction {

		public ShowTradingDataDialogAction(final JFileChooser fileChooser) {
			super(TRADINGDATA_KIND,  ApplicationContext.getInstance().getAppStatusBean());
			putValue(ACTION_COMMAND_KEY, "ShowTradingDataDialog");
			putValue(NAME, "Select File...");
			putValue(SHORT_DESCRIPTION, "Select an Orders file for Data Fusion");
			this.fileChooser = fileChooser;
		}

		protected JFileChooser getCustomisedFileChooser() {
			LOGGER.debug("Preparing a file chooser dialog to select an Orders file...");
			fileChooser.setDialogTitle(FILE_CHOOSER_TITLE_TRADINGDATA);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(tradingDataDirectory);
			fileChooser.setFileFilter(new OrdersFileFilter());
			return fileChooser;
		}

		protected Container getDialogParent() {
			return getParent();
		}

		protected void storeFile(final File ordersFile) {
			ordersFilename = ordersFile.getName();
			txtSelectedFile.setText(ordersFilename);
			btnRunDataFusion.setEnabled(true);
		}

		// -------- Private ----------

		private class OrdersFileFilter extends FileFilter {

			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(DataAccessUtils.ORDERS_FILE_SUFFIX);
			}

			@Override
			public String getDescription() {
				return ORDERSDATA_FILES;
			}

			// -------- Private ----------

		}

		private static final String TRADINGDATA_KIND = "Trading Data file";
		private static final String FILE_CHOOSER_TITLE_TRADINGDATA = "Select a Trading Data file...";
		private static final String ORDERSDATA_FILES = "Strategy orders files";

		private final JFileChooser fileChooser;
	}
	
	private class RunDataFusionToolAction extends AbstractAction {

		public RunDataFusionToolAction(final Object caller) {
			putValue(ACTION_COMMAND_KEY, "RunDataFusionTool");
			putValue(NAME, "Fuse Data");
			putValue(SHORT_DESCRIPTION, "Run Data Fusion Tool on selected Orders file...");
			this.caller = caller;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == caller) {
				try {
					dataFusionTool.setup(ordersFilename);
					dataFusionTool.fuse();
					ApplicationContext.getInstance().getAppStatusBean().setStatus("Data fusion succeeded for " + ordersFilename);
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
				finally {
					btnRunDataFusion.setEnabled(false);
				}
			}
		}

		private final Object caller;
	}

	private void initialise(final JFileChooser fileChooser) {
		BorderLayout borderLayout = (BorderLayout) getLayout();
		borderLayout.setVgap(10);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setToolTipText("Data Fusion Tool");
		{
			txtSelectedFile.setAlignmentX(Component.LEFT_ALIGNMENT);
			txtSelectedFile.setToolTipText("Name of selected Orders file");
			txtSelectedFile.setEditable(false);
			txtSelectedFile.setHorizontalAlignment(SwingConstants.LEFT);
			txtSelectedFile.setText("No file selected yet...");
			txtSelectedFile.setColumns(25);

			btnSelectFile.setAlignmentX(Component.RIGHT_ALIGNMENT);
			btnSelectFile.setToolTipText("Select an Orders file...");
			btnSelectFile.setHorizontalAlignment(SwingConstants.CENTER);
			btnSelectFile.setAction(new ShowTradingDataDialogAction(fileChooser));

			btnRunDataFusion.setAlignmentX(Component.CENTER_ALIGNMENT);
			btnRunDataFusion.setToolTipText("Run Data Fusion Tool on selected Orders file...");
			btnRunDataFusion.setHorizontalAlignment(SwingConstants.CENTER);
			btnRunDataFusion.setAction(new RunDataFusionToolAction(btnRunDataFusion));
			btnRunDataFusion.setEnabled(false);

			{
				JPanel northPanel = new JPanel();
				add(northPanel, BorderLayout.NORTH);
				GroupLayout groupLayout = new GroupLayout(northPanel);
				northPanel.setLayout(groupLayout);


				groupLayout.setHorizontalGroup(
					groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnSelectFile, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
							.addGap(3)
							.addComponent(txtSelectedFile, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE))
				);
				groupLayout.setVerticalGroup(
					groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup().addGap(5).addComponent(btnSelectFile))
						.addGroup(groupLayout.createSequentialGroup().addGap(5).addComponent(txtSelectedFile))
				);
			}
			{
				JPanel centrePanel = new JPanel();
				add(centrePanel, BorderLayout.CENTER);
				centrePanel.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
				GroupLayout groupLayout = new GroupLayout(centrePanel);
				centrePanel.setLayout(groupLayout);
				{
					JLabel lblL1QuoteFiltering = new JLabel("L1 Quote filtering:");
					JRadioButton rdbtnNoFiltering = new JRadioButton(L1QuoteFiltering.NO_FILTERING.toString());
					rdbtnNoFiltering.setActionCommand(L1QuoteFiltering.NO_FILTERING.toString());
					JRadioButton rdbtnVTransFiltering = new JRadioButton(L1QuoteFiltering.V_TRANSITIONS.toString(), true);
					rdbtnVTransFiltering.setActionCommand(L1QuoteFiltering.V_TRANSITIONS.toString());
					JRadioButton rdbtnPTransFiltering = new JRadioButton(L1QuoteFiltering.P_TRANSITIONS.toString());
					rdbtnPTransFiltering.setActionCommand(L1QuoteFiltering.P_TRANSITIONS.toString());

					JLabel lblLineEndingConvention = new JLabel("Line ending:");
					JRadioButton rdbtnCarrReturnLineFeed = new JRadioButton("\"\\r\\n\"", true);
					rdbtnCarrReturnLineFeed.setActionCommand(CRLF_LINE_SEP);
					JRadioButton rdbtnLineFeed = new JRadioButton("\"\\n\"");
					rdbtnLineFeed.setActionCommand(LF_LINE_SEP);

					groupLayout.setHorizontalGroup(
						groupLayout.createParallelGroup(Alignment.LEADING)
							.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(lblL1QuoteFiltering))
							.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(rdbtnNoFiltering))
							.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(rdbtnVTransFiltering))
							.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(rdbtnPTransFiltering))
							.addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(lblLineEndingConvention, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE))
							.addGroup(groupLayout.createSequentialGroup()
								.addContainerGap().addComponent(rdbtnCarrReturnLineFeed, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
								.addGap(3).addComponent(rdbtnLineFeed, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE))
					);
					groupLayout.setVerticalGroup(
						groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createSequentialGroup()
								.addContainerGap().addComponent(lblL1QuoteFiltering, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
								.addGap(3).addComponent(rdbtnNoFiltering, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
								.addComponent(rdbtnVTransFiltering, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
								.addComponent(rdbtnPTransFiltering, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE))
							.addGroup(groupLayout.createSequentialGroup()
								.addGap(10, 60, 32767).addComponent(lblLineEndingConvention))
								.addGroup(groupLayout.createParallelGroup()
									.addGap(3).addComponent(rdbtnCarrReturnLineFeed, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
									.addComponent(rdbtnLineFeed))
								.addContainerGap()
					);

					dataFusionTool.getL1QuoteFilterButtonGroup().add(rdbtnNoFiltering);
					dataFusionTool.getL1QuoteFilterButtonGroup().add(rdbtnVTransFiltering);
					dataFusionTool.getL1QuoteFilterButtonGroup().add(rdbtnPTransFiltering);

					dataFusionTool.getLineSeparatorButtonGroup().add(rdbtnCarrReturnLineFeed);
					dataFusionTool.getLineSeparatorButtonGroup().add(rdbtnLineFeed);
				}
			}
			{
				Box southHBox = Box.createHorizontalBox();
				add(southHBox, BorderLayout.SOUTH);
				{
					southHBox.add(Box.createHorizontalGlue());
					southHBox.add(btnRunDataFusion);
					southHBox.add(Box.createHorizontalGlue());
				}
			}
			add(Box.createVerticalBox().add(Box.createRigidArea(new Dimension(50, 100))), BorderLayout.WEST);
			add(Box.createVerticalBox().add(Box.createRigidArea(new Dimension(50, 100))), BorderLayout.EAST);
		}
	}

	private static final Logger LOGGER = LogManager.getLogger(DataFusionToolPanel.class);
	private static final String LF_LINE_SEP = "\n";
	private static final String CRLF_LINE_SEP = "\r\n";

	private final JTextField txtSelectedFile = new JTextField();
	private final JButton btnSelectFile = new JButton("Select File...");
	private final JButton btnRunDataFusion = new JButton("Fuse data...");
	private final DataFusionTool dataFusionTool = new DataFusionTool();

	private File tradingDataDirectory = null;
	private String ordersFilename;
}

package com.bormannqds.apps.wjh.main.gui;

import com.bormannqds.apps.wjh.datafusiontool.gui.DataFusionToolPanel;
import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.ptatool.gui.PtaToolPanel;

import com.bormannqds.lib.bricks.gui.StatusBar;

import javax.swing.*;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	/**
	 * Create the frame.
	 * 
	 * @param onExitAction
	 * @param hasSystemLnF
	 */
	public MainFrame(final Action onExitAction, boolean hasSystemLnF) {
		ApplicationContext.getInstance().setAppStatusBean(statusBar.getAppStatusBean());
		mainMenuBar = new MainMenuBar(onExitAction, fileChooser);
		dataFusionToolPanel = new DataFusionToolPanel(fileChooser);
		ptaToolPanel = new PtaToolPanel();
		if (!dataFusionToolPanel.isTradingDataDirectoryReady()) {
			statusBar.getAppStatusBean().setStatus("Problem locating trading data! Please check configuration.");
		}
		else if (!ptaToolPanel.isPtaDataTreeModelReady()) {
			statusBar.getAppStatusBean().setStatus("Problem reading PTA data directory structure! See log for details...");
		}
		else if (!hasSystemLnF) {
			statusBar.getAppStatusBean().setStatus("Ready without System L&F! Sorry for sticking out like a sore thumb :-(");
		}
		else {
			statusBar.getAppStatusBean().setStatus("Ready!");
		}
		initialise();
	}

	@Override
	public void dispose() {
		ptaToolPanel.dispose();
		super.dispose();
	}
	// -------- Private ----------

	private void initialise() {
        setTitle("What-Just-Happened Analyser");
		setName("Main Frame");
        setBounds(100, 100, 510, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(mainMenuBar);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        getContentPane().add(tabbedPane);
		tabbedPane.addTab(PTA_TAB_TITLE, null, ptaToolPanel, PTA_TAB_TIP);
		tabbedPane.addTab(DATAFUSION_TAB_TITLE, null, dataFusionToolPanel, DATAFUSION_TAB_TIP);
		getContentPane().add(statusBar);
		pack();
		setVisible(true);
	}

	private static final String DATAFUSION_TAB_TITLE = "Data Fusion";
	private static final String DATAFUSION_TAB_TIP = "Tool to fuse trading data with market data";
	private static final String PTA_TAB_TITLE = "Post-trade Analysis";
	private static final String PTA_TAB_TIP = "Tool to visualise fused data for Post-trade Analysis";

	// main widgets
	private final StatusBar statusBar = new StatusBar();
	private final MainMenuBar mainMenuBar;
	private final DataFusionToolPanel dataFusionToolPanel;
	private final PtaToolPanel ptaToolPanel;

	// utility widgets
	private final JFileChooser fileChooser = new JFileChooser();
	private JTabbedPane tabbedPane;
}

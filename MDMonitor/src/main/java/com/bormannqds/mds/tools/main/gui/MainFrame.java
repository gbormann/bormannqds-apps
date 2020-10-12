package com.bormannqds.mds.tools.main.gui;

import com.bormannqds.mds.lib.gateway.ApplicationContext;
import com.bormannqds.mds.tools.indicatortool.gui.IndicatorToolPanel;
import com.bormannqds.mds.tools.monitoringtool.MonitoringTool;
import com.bormannqds.mds.tools.monitoringtool.gui.MonitoringToolPanel;
import com.bormannqds.lib.bricks.gui.StatusBar;

import javax.swing.*;

/**
 * Created by bormanng on 6/07/15.
 */
public class MainFrame extends JFrame {
    /**
     * Create the frame.
     *  @param onStartAction
     * @param onSuspendAction
     * @param onExitAction
     * @param monitoringTool
     * @param hasSystemLnF
     */
    public MainFrame(final Action onStartAction, final Action onSuspendAction, final Action onExitAction, final MonitoringTool monitoringTool, boolean hasSystemLnF) {
        ApplicationContext.getInstance().setAppStatusBean(statusBar.getAppStatusBean());
        mainMenuBar = new MainMenuBar(onStartAction, onSuspendAction, onExitAction, fileChooser);
        monitoringToolPanel = new MonitoringToolPanel(monitoringTool.getMarketDataModel());
        if (!hasSystemLnF) {
            statusBar.getAppStatusBean().setStatus("Ready without System L&F! Sorry for sticking out like a sore thumb :-(");
        }
        else {
            statusBar.getAppStatusBean().setStatus("Ready!");
        }
        initialise();
    }

    @Override
    public void dispose() {
//        indicatorToolPanel.dispose();
        super.dispose();
    }

    public void armDataDisplayAction() {
        mainMenuBar.armDataDisplayAction();
    }

    public void disArmDataDisplayAction() {
        mainMenuBar.disArmDataDisplayAction();
    }

    // -------- Private ----------

    private void initialise() {
        setName("Main Frame");
        setTitle("Market Data Monitor");
        setBounds(100, 100, 510, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(mainMenuBar);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        getContentPane().add(tabbedPane);
        tabbedPane.addTab(MONITORING_TAB_TITLE, null, monitoringToolPanel, MONITORING_TAB_TIP);
        tabbedPane.addTab(INDICATORS_TAB_TITLE, null, indicatorToolPanel, INDICATORS_TAB_TIP);
        getContentPane().add(statusBar);
        pack();
        setVisible(true);
    }

    private static final String MONITORING_TAB_TITLE = "Monitoring";
    private static final String MONITORING_TAB_TIP = "Panel showing the market data in real-time";
    private static final String INDICATORS_TAB_TITLE = "Indicators";
    private static final String INDICATORS_TAB_TIP = "Tool to configure market data condition indicators";

    // main widgets
    private final StatusBar statusBar = new StatusBar();
    private final MainMenuBar mainMenuBar;
    private final JPanel monitoringToolPanel;
    private final JPanel indicatorToolPanel = new IndicatorToolPanel();

    // utility widgets
    private final JFileChooser fileChooser = new JFileChooser();
    private JTabbedPane tabbedPane;
}

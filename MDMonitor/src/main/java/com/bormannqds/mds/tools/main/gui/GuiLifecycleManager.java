package com.bormannqds.mds.tools.main.gui;

import com.bormannqds.mds.lib.gateway.ApplicationContext;
import com.bormannqds.mds.lib.protocoladaptor.Connection;

import com.bormannqds.mds.tools.main.MarketDataProcessor;
import com.bormannqds.mds.tools.monitoringtool.MonitoringTool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * NOTE: Make sure anything touching an object of this lives on the EDT only!!
 *
 * Created by bormanng on 15/07/15.
 */
public class GuiLifecycleManager implements Runnable {
    public GuiLifecycleManager(boolean hasSystemLnF, MonitoringTool monitoringTool, Connection mdpCtrlConn) {
        this.hasSystemLnF = hasSystemLnF;
        this.monitoringTool = monitoringTool;
        this.mdpCtrlConn = mdpCtrlConn;
    }

    /**
     * TODO Indicate which resource needs reloading before arming the Start... menu item
     */
    @Override
    public void run() {
        ZMQ.Socket mdpCtrlSocket = Connection.createSocket(ZMQ.REQ);
        mdpCtrlSocket.connect(mdpCtrlConn.getAddress());

        try {
            frame = new MainFrame(new OnResumeAction(), new OnSuspendAction(), new OnExitAction(), monitoringTool, hasSystemLnF);
            if (ApplicationContext.getInstance().getReferenceDataResource().isOpen()
                    && ApplicationContext.getInstance().getMulticastConfigResource().isOpen()
                    && ApplicationContext.getInstance().getOutRightsRollSchedulesResource().isOpen()
                    && ApplicationContext.getInstance().getSpreadsRollSchedulesResource().isOpen()) {
                if (controlMarketDataProcessor(MarketDataProcessor.START_CMD, MarketDataProcessor.READY_REPLY, mdpCtrlSocket)) {
                    ApplicationContext.getInstance().getAppStatusBean().setStatus("Market data display started...");
                    frame.disArmDataDisplayAction();
                }
                else {
                    LOGGER.fatal("Unresolvable problem with market data processor, giving up...");
                    ApplicationContext.getInstance().getAppStatusBean().setStatus("Market data processor not responding, giving up...");
                    mdpCtrlSocket.close();
                    System.exit(1);
                }
            }
            else {
                LOGGER.fatal("'Start from GUI nyi:' cannot start market data processor due to incomplete configuration, giving up...");
                ApplicationContext.getInstance().getAppStatusBean().setStatus("'Start from GUI nyi:' cannot start market data processor due to incomplete configuration, giving up...");
                mdpCtrlSocket.close();
                System.exit(2);
            }
        }
        catch (Exception e) {
            LOGGER.catching(e);
        }
        mdpCtrlSocket.close();
    }

    // -------- Private ----------

    private class OnResumeAction extends AbstractAction {
        public OnResumeAction() {
            putValue(ACTION_COMMAND_KEY, "Resume");
            putValue(NAME, "Resume");
            putValue(SHORT_DESCRIPTION, "Start data display");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ZMQ.Socket mdpCtrlSocket = Connection.createSocket(ZMQ.REQ);
            mdpCtrlSocket.connect(mdpCtrlConn.getAddress());

            if (controlMarketDataProcessor(MarketDataProcessor.RESUME_CMD, MarketDataProcessor.READY_REPLY, mdpCtrlSocket)) {
                frame.disArmDataDisplayAction();
                LOGGER.info("Market data display resumed...");
                ApplicationContext.getInstance().getAppStatusBean().setStatus("Market data display resumed...");
            }
            else {
                LOGGER.error("Market data processor not responding...");
                ApplicationContext.getInstance().getAppStatusBean().setStatus("Market data processor not responding...");
            }

            mdpCtrlSocket.close();
        }
    }

    private class OnSuspendAction extends AbstractAction {
        public OnSuspendAction() {
            putValue(ACTION_COMMAND_KEY, "Suspend");
            putValue(NAME, "Suspend");
            putValue(SHORT_DESCRIPTION, "Suspend data display");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ZMQ.Socket mdpCtrlSocket = Connection.createSocket(ZMQ.REQ);
            mdpCtrlSocket.connect(mdpCtrlConn.getAddress());

            if (controlMarketDataProcessor(MarketDataProcessor.SUSPEND_CMD, MarketDataProcessor.DONE_REPLY, mdpCtrlSocket)) {
                frame.armDataDisplayAction();
                LOGGER.info("Market data display suspended...");
                ApplicationContext.getInstance().getAppStatusBean().setStatus("Market data display suspended...");
            }
            else {
                LOGGER.error("Market data processor not responding...");
                ApplicationContext.getInstance().getAppStatusBean().setStatus("Market data processor not responding...");
            }

            mdpCtrlSocket.close();
        }
    }

    private class OnExitAction extends AbstractAction {
        public OnExitAction() {
            putValue(ACTION_COMMAND_KEY, "Exit");
            putValue(NAME, "Exit");
            putValue(SHORT_DESCRIPTION, "Exit the application");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ApplicationContext.getInstance().getAppStatusBean().setStatus("Shutting down...");
            ZMQ.Socket mdpCtrlSocket = Connection.createSocket(ZMQ.REQ);
            mdpCtrlSocket.connect(mdpCtrlConn.getAddress());

            controlMarketDataProcessor(MarketDataProcessor.STOP_CMD, MarketDataProcessor.DONE_REPLY, mdpCtrlSocket);
            frame.dispose();
            mdpCtrlSocket.close();
        }
    }

    /**
     * control market data processor
     */
    private static boolean controlMarketDataProcessor(final String cmd, final String expect, final ZMQ.Socket mdpCtrlSocket) {
        final ZMsg reqMsg = new ZMsg();
        reqMsg.add(cmd);
        reqMsg.send(mdpCtrlSocket);
        final ZMsg responseMsg = ZMsg.recvMsg(mdpCtrlSocket);
        if (responseMsg == null) {
            LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
            return false;
        }
        final String response = responseMsg.popString();
        if (!response.equals(expect)) {
            LOGGER.warn(response + ": market data processor is mumbling");
            return false; // don't trust a mumbling bridge
        }
        responseMsg.destroy();
        LOGGER.debug("response: "+response);
        return true;
    }

    private static final Logger LOGGER = LogManager.getLogger(GuiLifecycleManager.class);

    private final boolean hasSystemLnF;
    private final MonitoringTool monitoringTool;
    private final Connection mdpCtrlConn;
    private MainFrame frame; // Main frame with Menu and Data Fusion Tool panel
}

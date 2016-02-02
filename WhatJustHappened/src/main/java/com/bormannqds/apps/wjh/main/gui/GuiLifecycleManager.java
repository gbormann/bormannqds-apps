package com.bormannqds.apps.wjh.main.gui;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * NOTE: Make sure anything touching an object of this lives on the EDT only!!
 *
 * Created by bormanng on 25/07/15.
 */
public class GuiLifecycleManager implements Runnable {
    public GuiLifecycleManager(boolean hasSystemLnF) {
        this.hasSystemLnF = hasSystemLnF;
    }

    @Override
    public void run() {
        try {
            frame = new MainFrame(new OnExitAction(), hasSystemLnF);
        }
        catch (Exception e) {
            LOGGER.catching(e);
        }
    }

    // -------- Private ----------

    private class OnExitAction extends AbstractAction {
        public OnExitAction() {
            putValue(ACTION_COMMAND_KEY, "Exit");
            putValue(NAME, "Exit");
            putValue(SHORT_DESCRIPTION, "Exit the application");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ApplicationContext.getInstance().getAppStatusBean().setStatus("Shutting down...");
            frame.dispose();

            System.exit(0); // main thread has died by now, so instigate system exit from here
        }
    }

    private static final Logger LOGGER = LogManager.getLogger(GuiLifecycleManager.class);

    private final boolean hasSystemLnF;
    private MainFrame frame; // Main frame with Menu and Data Fusion Tool panel
}

package com.bormannqds.mds.tools.main.gui;

import com.bormannqds.mds.lib.gateway.ApplicationContext;
import com.bormannqds.lib.bricks.gui.AbstractShowOpenFileDialogAction;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("serial")
class MainMenuBar extends JMenuBar {
    public static final String SHOW_REFDATA_DIALOG = "ShowRefDataDialog";
    public static final String SHOW_MCCONFIG_DIALOG = "ShowMcConfigDialog";
    public static final String SHOW_OUTR_ROLLSCHEDS_DIALOG = "ShowOutrRollSchedsDialog";
    public static final String SHOW_SPRD_ROLLSCHEDS_DIALOG = "ShowSprdRollSchedsDialog";

    public MainMenuBar(final Action onStartAction,
                       final Action onSuspendAction,
                       final Action onExitAction,
                       final JFileChooser fileChooser) {
		initialise(onStartAction, onSuspendAction, onExitAction, fileChooser);
	}

    public void armDataDisplayAction() {
        resumeDataDisplayItem.setEnabled(true);
        suspendDataDisplayItem.setEnabled(false);
    }

    public void disArmDataDisplayAction() {
        resumeDataDisplayItem.setEnabled(false);
        suspendDataDisplayItem.setEnabled(true);
    }

    // -------- Private ----------

	private class ShowRefDataDialogAction extends AbstractShowOpenFileDialogAction {
		public ShowRefDataDialogAction(final JFileChooser fileChooser) {
			super(REFDATA_KIND, ApplicationContext.getInstance().getAppStatusBean());
			putValue(ACTION_COMMAND_KEY, SHOW_REFDATA_DIALOG);
			putValue(NAME, "Load Reference Data...");
			putValue(SHORT_DESCRIPTION, "Load Reference Data from an XML file");
			this.fileChooser = fileChooser;
		}

		@Override
		protected JFileChooser getCustomisedFileChooser() {
			LOGGER.debug("Preparing a file chooser dialog to load the reference data xml file...");
			fileChooser.setDialogTitle(FILE_CHOOSER_TITLE_REFDATA);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(ApplicationContext.getInstance().getApplicationWorkingDirectory().toFile());
			return fileChooser;
		}

        @Override
		protected Container getDialogParent() {
			return getParent();
		}

        @Override
		protected void storeFile(final File file) {
			try {
				URL fileUrl = file.toURI().toURL();
				ApplicationContext.getInstance().getReferenceDataResource().setLocator(fileUrl);
			}
			catch (MalformedURLException mue) {
				LOGGER.error("BUG: Failed conversion of file path to URL from java.io.File object: should not happen!", mue);
			}
            catch (ResourceIOException rioe) {
                // TODO change message if Reference Data Resource implementation of reset() changes
				LOGGER.error("BUG: Should not happen because the generating method is a NOP!", rioe);
			}
		}

		private static final String REFDATA_KIND = "Reference Data file";
		private static final String FILE_CHOOSER_TITLE_REFDATA = "Select a Reference Data file...";

		private final JFileChooser fileChooser;
	}

    private class ShowMcConfigDialogAction extends AbstractShowOpenFileDialogAction {
        public ShowMcConfigDialogAction(final JFileChooser fileChooser) {
            super(MCCONFIG_KIND, ApplicationContext.getInstance().getAppStatusBean());
            putValue(ACTION_COMMAND_KEY, SHOW_MCCONFIG_DIALOG);
            putValue(NAME, "Load Multicast Configs...");
            putValue(SHORT_DESCRIPTION, "Load Multicast Configurations from an XML file");
            this.fileChooser = fileChooser;
        }

        @Override
        protected JFileChooser getCustomisedFileChooser() {
            LOGGER.debug("Preparing a file chooser dialog to load the multicast configurations xml file...");
            fileChooser.setDialogTitle(FILE_CHOOSER_TITLE_MCCONFIG);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setCurrentDirectory(ApplicationContext.getInstance().getApplicationWorkingDirectory().toFile());
            return fileChooser;
        }

        @Override
        protected Container getDialogParent() {
            return getParent();
        }

        @Override
        protected void storeFile(final File file) {
            try {
                URL fileUrl = file.toURI().toURL();
                ApplicationContext.getInstance().getMulticastConfigResource().setLocator(fileUrl);
            }
            catch (MalformedURLException mue) {
                LOGGER.error("BUG: Failed conversion of file path to URL from java.io.File object: should not happen!", mue);
            }
            catch (ResourceIOException rioe) {
                // TODO change message if Multicast Config Resource implementation of reset() changes
                LOGGER.error("BUG: Should not happen because the generating method is a NOP!", rioe);
            }
        }

        private static final String MCCONFIG_KIND = "Multicast Configurations file";
        private static final String FILE_CHOOSER_TITLE_MCCONFIG = "Select a Multicast Configurations file...";

        private final JFileChooser fileChooser;
    }

    private class ShowOutrRollSchedsDialogAction extends AbstractShowOpenFileDialogAction {
        public ShowOutrRollSchedsDialogAction(final JFileChooser fileChooser) {
            super(OUTR_ROLLSCHEDS_KIND, ApplicationContext.getInstance().getAppStatusBean());
            putValue(ACTION_COMMAND_KEY, SHOW_OUTR_ROLLSCHEDS_DIALOG);
            putValue(NAME, "Load Outrights Roll Schedules...");
            putValue(SHORT_DESCRIPTION, "Load Roll Schedules for Outrights from an XML file");
            this.fileChooser = fileChooser;
        }

        @Override
        protected JFileChooser getCustomisedFileChooser() {
            LOGGER.debug("Preparing a file chooser dialog to load the outrights roll schedules xml file...");
            fileChooser.setDialogTitle(FILE_CHOOSER_TITLE_OUTR_ROLLSCHEDS);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setCurrentDirectory(ApplicationContext.getInstance().getApplicationWorkingDirectory().toFile());
            return fileChooser;
        }

        @Override
        protected Container getDialogParent() {
            return getParent();
        }

        @Override
        protected void storeFile(final File file) {
            try {
                URL fileUrl = file.toURI().toURL();
                ApplicationContext.getInstance().getOutRightsRollSchedulesResource().setLocator(fileUrl);
            }
            catch (MalformedURLException mue) {
                LOGGER.error("BUG: Failed conversion of file path to URL from java.io.File object: should not happen!", mue);
            }
            catch (ResourceIOException rioe) {
                // TODO change message if Outrights Roll Schedules Resource implementation of reset() changes
                LOGGER.error("BUG: Should not happen because the generating method is a NOP!", rioe);
            }
        }

        private static final String OUTR_ROLLSCHEDS_KIND = "Outrights Roll Schedules file";
        private static final String FILE_CHOOSER_TITLE_OUTR_ROLLSCHEDS = "Select an Outrights Roll Schedules file...";

        private final JFileChooser fileChooser;
    }

    private class ShowSprdRollSchedsDialogAction extends AbstractShowOpenFileDialogAction {
        public ShowSprdRollSchedsDialogAction(final JFileChooser fileChooser) {
            super(SPRD_ROLLSCHEDS_KIND, ApplicationContext.getInstance().getAppStatusBean());
            putValue(ACTION_COMMAND_KEY, SHOW_SPRD_ROLLSCHEDS_DIALOG);
            putValue(NAME, "Load Spread Roll Schedules...");
            putValue(SHORT_DESCRIPTION, "Load Roll Schedules for Spreads from an XML file");
            this.fileChooser = fileChooser;
        }

        @Override
        protected JFileChooser getCustomisedFileChooser() {
            LOGGER.debug("Preparing a file chooser dialog to load the spreads roll schedules xml file...");
            fileChooser.setDialogTitle(FILE_CHOOSER_TITLE_SPRD_ROLLSCHEDS);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setCurrentDirectory(ApplicationContext.getInstance().getApplicationWorkingDirectory().toFile());
            return fileChooser;
        }

        @Override
        protected Container getDialogParent() {
            return getParent();
        }

        @Override
        protected void storeFile(final File file) {
            try {
                URL fileUrl = file.toURI().toURL();
                ApplicationContext.getInstance().getSpreadsRollSchedulesResource().setLocator(fileUrl);
            }
            catch (MalformedURLException mue) {
                LOGGER.error("BUG: Failed conversion of file path to URL from java.io.File object: should not happen!", mue);
            } catch (ResourceIOException rioe) {
                // TODO change message if Spreads Roll Schedules Resource implementation of reset() changes
                LOGGER.error("BUG: Should not happen because the generating method is a NOP!", rioe);
            }
        }

        private static final String SPRD_ROLLSCHEDS_KIND = "Spread Roll Schedules file";
        private static final String FILE_CHOOSER_TITLE_SPRD_ROLLSCHEDS = "Select a Spread Roll Schedules file...";

        private final JFileChooser fileChooser;
    }

    private void initialise(final Action onResumeAction, final Action onSuspendAction, final Action onExitAction, final JFileChooser fileChooser) {
		{
			final JMenu fileMenu = new JMenu("File");
			fileMenu.setMnemonic(KeyEvent.VK_F);
			add(fileMenu);
			{
                final JMenuItem loadRefDataItem = new JMenuItem(new ShowRefDataDialogAction(fileChooser));
				fileMenu.add(loadRefDataItem);
				loadRefDataItem.setMnemonic(KeyEvent.VK_R);				
			}
            {
                final JMenuItem loadMcConfigItem = new JMenuItem(new ShowMcConfigDialogAction(fileChooser));
                fileMenu.add(loadMcConfigItem);
                loadMcConfigItem.setMnemonic(KeyEvent.VK_M);
            }
            {
                final JMenuItem loadOutrRollSchedsItem = new JMenuItem(new ShowOutrRollSchedsDialogAction(fileChooser));
                fileMenu.add(loadOutrRollSchedsItem);
                loadOutrRollSchedsItem.setMnemonic(KeyEvent.VK_O);
            }
            {
                final JMenuItem loadSprdRollSchedsItem = new JMenuItem(new ShowSprdRollSchedsDialogAction(fileChooser));
                fileMenu.add(loadSprdRollSchedsItem);
                loadSprdRollSchedsItem.setMnemonic(KeyEvent.VK_S);
            }
			{
				JMenuItem exitItem = new JMenuItem(onExitAction);
				fileMenu.add(exitItem);
				exitItem.setMnemonic(KeyEvent.VK_X);
			}
		}
        {
            JMenu fileMenu = new JMenu("Connect");
            fileMenu.setMnemonic(KeyEvent.VK_T);
            add(fileMenu);
            {
                resumeDataDisplayItem = new JMenuItem(onResumeAction);
                fileMenu.add(resumeDataDisplayItem);
                resumeDataDisplayItem.setMnemonic(KeyEvent.VK_S);
                resumeDataDisplayItem.setEnabled(false);
            }
            {
                suspendDataDisplayItem = new JMenuItem(onSuspendAction);
                fileMenu.add(suspendDataDisplayItem);
                suspendDataDisplayItem.setMnemonic(KeyEvent.VK_S);
                suspendDataDisplayItem.setEnabled(false);
            }
        }
		{
			JMenu windowMenu = new JMenu("Window");
			add(windowMenu);
			windowMenu.setMnemonic(KeyEvent.VK_W);
			{
				JMenuItem baseTickerPickerItem = new JMenuItem();
				windowMenu.add(baseTickerPickerItem);
				baseTickerPickerItem.setMnemonic(KeyEvent.VK_B);
				baseTickerPickerItem.setEnabled(false);
			}
		}
	}

	private static final Logger LOGGER = LogManager.getLogger(MainMenuBar.class);//LogFactory.getFactory().getLog(LaunchAction.class);

    private JMenuItem resumeDataDisplayItem;
    private JMenuItem suspendDataDisplayItem;
}

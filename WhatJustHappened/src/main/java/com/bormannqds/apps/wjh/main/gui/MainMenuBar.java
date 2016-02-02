package com.bormannqds.apps.wjh.main.gui;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;

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

	public MainMenuBar(final Action onExitAction, final JFileChooser fileChooser) {
//		this.ptaChartDialog = new PtaChartDialog(); // Post-Trade Analysis tool dialog
		initialise(onExitAction, fileChooser);
	}

	// -------- Private ----------

	private class ShowRefDataDialogAction extends AbstractShowOpenFileDialogAction {
		public ShowRefDataDialogAction(final JFileChooser fileChooser) {
			super(REFDATA_KIND, ApplicationContext.getInstance().getAppStatusBean());
			putValue(ACTION_COMMAND_KEY, "ShowRefDataDialog");
			putValue(NAME, "Load Reference Data...");
			putValue(SHORT_DESCRIPTION, "Load Reference Data from an XML file");
			this.fileChooser = fileChooser;
		}

		protected JFileChooser getCustomisedFileChooser() {
			LOGGER.debug("Preparing a file chooser dialog to load the reference data xml file...");
			fileChooser.setDialogTitle(FILE_CHOOSER_TITLE_REFDATA);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(ApplicationContext.getInstance().getApplicationWorkingDirectory().toFile());
			return fileChooser;
		}

		protected Container getDialogParent() {
			return getParent();
		}

		protected void storeFile(final File file) {
			try {
				URL fileUrl = file.toURI().toURL();
				ApplicationContext.getInstance().getReferenceDataResource().setLocator(fileUrl);
			}
			catch (MalformedURLException mue) {
				LOGGER.error("BUG: Failed conversion of file path to URL from java.io.File object: should not happen!", mue);
			} catch (ResourceIOException rioe) {
				 // TODO change message if Ref Data Rsrc implementation of reset() changes
				LOGGER.error("BUG: Should not happen because the generating method is a NOP!", rioe);
			}
		}

		private static final String REFDATA_KIND = "Reference Data file";
		private static final String FILE_CHOOSER_TITLE_REFDATA = "Select a Reference Data file...";

		private final JFileChooser fileChooser;
	}

	private void initialise(final Action onExitAction, final JFileChooser fileChooser) {
		{
			JMenu fileMenu = new JMenu("File");
			fileMenu.setMnemonic(KeyEvent.VK_F);
			add(fileMenu);
			{
				JMenuItem loadRefDataItem = new JMenuItem(new ShowRefDataDialogAction(fileChooser));
				fileMenu.add(loadRefDataItem);
				loadRefDataItem.setMnemonic(KeyEvent.VK_R);				
			}
			{
				JMenuItem exitItem = new JMenuItem(onExitAction);
				fileMenu.add(exitItem);
				exitItem.setMnemonic(KeyEvent.VK_X);
			}
		}
		{
			JMenu windowMenu = new JMenu("Window");
			add(windowMenu);
			windowMenu.setMnemonic(KeyEvent.VK_W);
			{
				JMenuItem ptaToolItem = new JMenuItem();
				windowMenu.add(ptaToolItem);
				ptaToolItem.setMnemonic(KeyEvent.VK_P);
				ptaToolItem.setEnabled(false);
			}
		}
	}

	private static final Logger LOGGER = LogManager.getLogger(MainMenuBar.class);//LogFactory.getFactory().getLog(LaunchAction.class);

//	private final PtaChartDialog ptaChartDialog; // Post-Trade Analysis tool dialog
}

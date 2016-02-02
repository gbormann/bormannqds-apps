package com.bormannqds.apps.wjh.main;

import com.bormannqds.apps.wjh.lib.configuration.ConfigurationResource;
import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.referencedata.ReferenceDataResource;
import com.bormannqds.apps.wjh.main.gui.GuiLifecycleManager;

import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.utils.system.FileSystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@SuppressWarnings("serial")
public class WhatJustHappened {
//public:
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		if (GraphicsEnvironment.isHeadless()) {
			LOGGER.warn("This application's GUI needs a desktop environment!");
			System.exit(1);
		}

		boolean hasSystemLnF = false;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			hasSystemLnF = true;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
			LOGGER.warn("Unfortunately this app will stick out like a sore thumb. Setup of System L&F failed: ", ex);
		} catch (UnsupportedLookAndFeelException ulfe) {
			LOGGER.warn("Unfortunately this app will stick out like a sore thumb. It appears the System L&F is not supported with current Java version: ", ulfe);
		}

		ConfigurationResource configResource = null;
		boolean exit = true;
		Path cwd = Paths.get("");//.normalize();
//		Path cwd = Paths.get(System.getProperty("user.dir"));
		Path configFilePath = cwd.resolve("etc/WjhConfig.xml");
		URL configUrl = null;
		try {
			if (Files.notExists(configFilePath)) {
				BufferedWriter newConfigWriter = Files.newBufferedWriter(configFilePath, Charset.forName("UTF-8"), StandardOpenOption.CREATE_NEW);
				newConfigWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				newConfigWriter.newLine();
				newConfigWriter.write("<configuration/>");
				newConfigWriter.close();
			}

			if (!Files.isRegularFile(configFilePath)){
				LOGGER.fatal(configFilePath + " is not a regular file!");
			}
			else if (!(Files.isReadable(configFilePath)
					&& Files.isWritable(configFilePath))) {
				LOGGER.fatal(configFilePath + " is not read/writeable!");				
			}
			else {
				configUrl = configFilePath.toUri().toURL();
				configResource = new ConfigurationResource(configUrl);
				configResource.open();
				if (configResource.isOpen()) {
					exit = false;
				}
			}
		}
		catch(MalformedURLException mue) {
			LOGGER.fatal("BUG: Malformed URL to configuration file: " + configUrl, mue);
		}
		catch(IOException ioe) {
			LOGGER.fatal("Could not create/open/load configuration file: " + configFilePath, ioe);
		}

		if (exit) {
			System.exit(1);			
		}
		exit = true;

		URL outputLocator = configResource.getOutputBaseLocator();
		Path outputPath = null;
		try {
			outputPath = Paths.get(outputLocator.toURI());
			FileSystemUtils.createRWDirectory(outputPath);
			exit = false;
		}
		catch (URISyntaxException use) {
			LOGGER.fatal("Malformed output base locator: " + outputLocator, use);
		}
		catch (IOException ioe) {
			LOGGER.fatal("Could not create output directory: " + outputPath, ioe);
		}

		if (exit) {
			System.exit(1);			
		}

		WhatJustHappened app = new WhatJustHappened(cwd.toUri(), configResource, hasSystemLnF);
		SwingUtilities.invokeLater(app.getGuiLifecycleManager());

        // main thread dies here
	}

	/**
	 * Create the application.
	 * @param hasSystemLnF 
	 * @throws URISyntaxException 
	 */
	public WhatJustHappened(final URI appWorkingDirectoryUri, final ConfigurationResource configResource, boolean hasSystemLnF) {
		URL refDataResourceLocator = configResource.getRefDataResourceLocator();
		ReferenceDataResource refDataResource;
		if (refDataResourceLocator == null) {
			refDataResource = new ReferenceDataResource();			
		}
		else {
			refDataResource = new ReferenceDataResource(refDataResourceLocator);
			try {
				refDataResource.open();
			} catch (ResourceIOException rioe) {
				LOGGER.fatal("Error whilst trying to load the Reference Data file! Please fix file or remove configuration...", rioe);
				System.exit(1);
			}
		}
		ApplicationContext.createInstance(Paths.get(appWorkingDirectoryUri), configResource, refDataResource);
        guiLifecycleManager = new GuiLifecycleManager(hasSystemLnF);
	}

    public GuiLifecycleManager getGuiLifecycleManager() {
        return guiLifecycleManager;
    }

	// -------- Private ----------

	private static final Logger LOGGER = LogManager.getLogger(WhatJustHappened.class);

    private final GuiLifecycleManager guiLifecycleManager;
}

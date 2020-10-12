package com.bormannqds.mds.tools.main;

import com.bormannqds.mds.tools.main.gui.GuiLifecycleManager;
import com.bormannqds.mds.tools.monitoringtool.MonitoringTool;

import com.bormannqds.mds.lib.configuration.ConfigurationResource;
import com.bormannqds.mds.lib.configuration.MulticastConfigResource;
import com.bormannqds.mds.lib.gateway.ApplicationContext;
import com.bormannqds.mds.lib.referencedata.ReferenceDataResource;
import com.bormannqds.mds.lib.referencedata.RollScheduleResource;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by bormanng on 10/06/15.
 */
public class MDMonitor {
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

        // PC: setUp application context (time to pick up Spring again)
        ConfigurationResource configResource = null;
        boolean exit = true;
        Path cwd = Paths.get("");//.normalize();
//		Path cwd = Paths.get(System.getProperty("user.dir"));
        Path configFilePath = cwd.resolve("etc/MdmConfig.xml");
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

        MDMonitor app = new MDMonitor(cwd.toUri(), configResource);
        app.launchMarketDataProcessor();
        SwingUtilities.invokeLater(new GuiLifecycleManager(hasSystemLnF, app.getMonitoringTool(), app.getMdProcessor().getCtrlConnection()));
        app.waitForMarketDataProcessor();
        try {
            ApplicationContext.getInstance().dispose();
        }
        catch(ResourceIOException rioe) {
            LOGGER.warn("Problems occurred when persisting uncommitted changes to one or more resources in the application context!", rioe);
            // TODO Display a warning msgbox
        }
        catch (InterruptedException ie) {
            LOGGER.warn("Someone got impatient waiting for the ZMQ reaper reaper thread to finish...");
        }
        System.exit(0);
    }

    /**
     * Create the application.
     *
     * @param appWorkingDirectoryUri
     * @param configResource
     */
    public MDMonitor(final URI appWorkingDirectoryUri, final ConfigurationResource configResource) {
        final ReferenceDataResource refDataResource;
        final MulticastConfigResource multicastConfigResource;
        final RollScheduleResource outrRollSchedResource;
        final RollScheduleResource sprdRollSchedResource;
        URL resourceLocator = configResource.getRefDataResourceLocator();
        if (resourceLocator == null) {
            refDataResource = new ReferenceDataResource();
        }
        else {
            refDataResource = new ReferenceDataResource(resourceLocator);
            try {
                refDataResource.open();
            } catch (ResourceIOException rioe) {
                LOGGER.fatal("Error whilst trying to load the Reference Data file! Please fix configuration or file...", rioe);
                System.exit(1);
            }
        }
        resourceLocator = configResource.getMulticastConfigResourceLocator();
        if (resourceLocator == null) {
            multicastConfigResource = new MulticastConfigResource();
        }
        else {
            multicastConfigResource = new MulticastConfigResource(resourceLocator);
            try {
                multicastConfigResource.open();
            } catch (ResourceIOException rioe) {
                LOGGER.fatal("Error whilst trying to load the Multicast Configurations file! Please fix configuration or file...", rioe);
                System.exit(1);
            }
        }
        resourceLocator = configResource.getOutrightsRollSchedulesResourceLocator();
        if (resourceLocator == null) {
            outrRollSchedResource = new RollScheduleResource();
        }
        else {
            outrRollSchedResource = new RollScheduleResource(resourceLocator);
            try {
                outrRollSchedResource.open();
            } catch (ResourceIOException rioe) {
                LOGGER.fatal("Error whilst trying to load the outrights roll schedule file! Please fix configuration or file...", rioe);
                System.exit(1);
            }
        }
        resourceLocator = configResource.getSpreadsRollSchedulesResourceLocator();
        if (resourceLocator == null) {
            sprdRollSchedResource = new RollScheduleResource();
        }
        else {
            sprdRollSchedResource = new RollScheduleResource(resourceLocator);
            try {
                sprdRollSchedResource.open();
            } catch (ResourceIOException rioe) {
                LOGGER.fatal("Error whilst trying to load the spreads roll schedule file! Please fix configuration or file...", rioe);
                System.exit(1);
            }
        }
        ApplicationContext.createInstance(Paths.get(appWorkingDirectoryUri),
                configResource,
                refDataResource,
                outrRollSchedResource,
                sprdRollSchedResource,
                multicastConfigResource);

        monitoringTool.initialise(ApplicationContext.getInstance().getConfigurationResource().getInterestList());
    }

    public MonitoringTool getMonitoringTool() {
        return monitoringTool;
    }

    public MarketDataProcessor getMdProcessor() {
        return mdProcessor;
    }

    // -------- Private ----------

    private boolean isMulticastConfigReady() {
        return ApplicationContext.getInstance().getMulticastConfigResource().isOpen();
    }

    private void launchMarketDataProcessor() {
        if (isMulticastConfigReady()) {
            mdProcessor.start("MD Proc"); // start processing market data on market data processor thread
        }
    }

    private void waitForMarketDataProcessor() {
        mdProcessor.join();
    }

    private static final Logger LOGGER = LogManager.getLogger(MDMonitor.class);

    private final MonitoringTool monitoringTool = new MonitoringTool();

    private final MarketDataProcessor mdProcessor = new MarketDataProcessor(monitoringTool);

    private GuiLifecycleManager guiLifecycleManager;
}

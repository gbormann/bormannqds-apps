package com.bormannqds.apps.wjh.lib.gateway;

import com.bormannqds.apps.wjh.lib.configuration.ConfigurationResource;
import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;
import com.bormannqds.apps.wjh.lib.resources.marketdata.MarketDataResource;
import com.bormannqds.apps.wjh.lib.resources.ptadata.PtaDataResource;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.TradingDataResource;
import com.bormannqds.apps.wjh.lib.referencedata.ReferenceDataResource;

import com.bormannqds.lib.bricks.gateway.BaseGuiApplicationContext;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Application context singleton
 * @author guy
 *
 */
public class ApplicationContext extends BaseGuiApplicationContext {

	public static void createInstance(final Path appWorkingDirectory,
										final ConfigurationResource configResource,
										final ReferenceDataResource refDataresource) {
		instance = new ApplicationContext(appWorkingDirectory, configResource, refDataresource);
	}

	public static ApplicationContext getInstance() {
		if (instance == null) {
			LOGGER.error("BUG: use of ApplicationContext instance before it is initialised!");
			throw new NullPointerException("BUG: use of ApplicationContext instance before it is initialised!");
		}
		
		return instance;
	}

	public ConfigurationResource getConfigurationResource() {
		return configResource;
	}

	public ReferenceDataResource getReferenceDataResource() {
		return refDataResource;
	}

	public MarketDataResource getMarketDataResource(final String ticker) {
		MarketDataResource mktDataResource = mdResourceCache.get(ticker);
		if (mktDataResource == null) {
			mktDataResource = new MarketDataResource(configResource.getMktDataResourceBaseLocator(), ticker);
			mdResourceCache.put(ticker, mktDataResource);
		}

		return mktDataResource;
	}

	public TradingDataResource getTradingDataResource(final String strategy) {
		TradingDataResource trdDataResource = tdResourceCache.get(strategy);
		if (trdDataResource == null) {
			trdDataResource = new TradingDataResource(configResource.getTrdDataResourceBaseLocator(), strategy);
			tdResourceCache.put(strategy, trdDataResource);
		}

		return trdDataResource;
	}

	public PtaDataResource getPtaDataResource(final String dateString, final String strategy) throws ResourceNotFoundException {
		String keyString = DataAccessUtils.createRelativePathname(dateString, strategy);
		PtaDataResource ptaDataResource = ptadResourceCache.get(keyString);
		if (ptaDataResource == null) {
			ptaDataResource = new PtaDataResource(configResource.getOutputBaseLocator(), dateString, strategy);
			ptadResourceCache.put(keyString, ptaDataResource);
		}

		return ptaDataResource;
	}

	public void dispose() throws ResourceIOException {
		configResource.close();
		refDataResource.close();
		mdResourceCache.clear();
		for (TradingDataResource trdDataResource: tdResourceCache.values()){
			trdDataResource.dispose();
		}
		tdResourceCache.clear();
		for (PtaDataResource ptaDataResource: ptadResourceCache.values()){
			ptaDataResource.dispose();
		}
		ptadResourceCache.clear();
	}

	// -------- Private ----------

	private ApplicationContext(final Path appWorkingDirectory,
								final ConfigurationResource configResource,
								final ReferenceDataResource refDataresource) {
        super(appWorkingDirectory);
		this.configResource = configResource;
		this.refDataResource = refDataresource;
	}

	private static final Logger LOGGER = LogManager.getLogger(ApplicationContext.class);
	private static ApplicationContext instance;

	private final ConfigurationResource configResource;
	private final ReferenceDataResource refDataResource;
	private final Map<String, MarketDataResource> mdResourceCache = new HashMap<String, MarketDataResource>();
	private final Map<String, TradingDataResource> tdResourceCache = new HashMap<String, TradingDataResource>();
	private final Map<String, PtaDataResource> ptadResourceCache = new HashMap<String, PtaDataResource>();
}

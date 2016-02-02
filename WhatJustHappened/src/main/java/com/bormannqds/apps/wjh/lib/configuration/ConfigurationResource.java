package com.bormannqds.apps.wjh.lib.configuration;

import com.bormannqds.apps.wjh.lib.resources.marketdata.L1QuoteFiltering;
import com.bormannqds.apps.wjh.lib.timeseries.TimeSeriesKeys;

import com.bormannqds.lib.dataaccess.resources.WritableXmlConfigurationResource;

import java.net.URL;
import java.util.regex.Matcher;

public class ConfigurationResource extends WritableXmlConfigurationResource {

	public ConfigurationResource(final URL locator) {
		super(locator);
	}

	public String getDefaultCsvRecordSeparator() {
		return fileConfig.getString(RECSEP_KEY, "\r\n").replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
	}

	public void setDefaultCsvRecordSeparator(final String newValue) {
		fileConfig.setProperty(RECSEP_KEY, newValue.replaceAll("\r", Matcher.quoteReplacement("\\r")).replaceAll("\n", Matcher.quoteReplacement("\\n")));
		setDirty();
	}

	public L1QuoteFiltering getDefaultQuoteFiltering() {
		return L1QuoteFiltering.valueOf(fileConfig.getString(QUOTEFILTERING_KEY));
	}

	public TimeSeriesKeys getModelVarToPlot() {
		return TimeSeriesKeys.valueOf(fileConfig.getString(MODELVARTOPLOT_KEY));
	}

	public URL getRefDataResourceLocator() {
		return getUrl(REFDATA_KEY, REFDATA_MSG);
	}

	public URL getOrdersDataLocator() {
		return getUrl(ORDERSDATA_KEY, ORDERSDATA_MSG);
	}

	public URL getMktDataResourceBaseLocator() {
		return getUrl(MKTDATA_KEY, MKTDATA_MSG);
	}

	public URL getTrdDataResourceBaseLocator() {
		return getUrl(TRDDATA_KEY, TRDDATA_MSG);
	}

	public URL getOutputBaseLocator() {
		return getUrl(OUTPUT_KEY, OUTPUT_MSG);
	}

	// -------- Private ----------

	private static final String RECSEP_KEY = "settings.csv.defaultRecordSeparator";
	private static final String QUOTEFILTERING_KEY = "settings.md.defaultQuoteFiltering";
	private static final String MODELVARTOPLOT_KEY = "settings.plot.modelVarToPlot";
	private static final String REFDATA_KEY = "env.files.referenceDataUrl";
	private static final String MKTDATA_KEY = "env.directories.marketDataUrl";
	private static final String TRDDATA_KEY = "env.directories.tradingDataUrl";
	private static final String OUTPUT_KEY = "env.directories.outputUrl";
	private static final String REFDATA_MSG = "Malformed URL to reference data file: "; // can go into a catalogue
	private static final String MKTDATA_MSG = "Malformed URL to market data directory: "; // can go into a catalogue
	private static final String TRDDATA_MSG = "Malformed URL to trading data directory: "; // can go into a catalogue
	private static final String OUTPUT_MSG = "Malformed URL to output directory: "; // can go into a catalogue

	private static final String ORDERSDATA_KEY = "env.files.ordersDataUrl";
	private static final String ORDERSDATA_MSG = "Malformed URL to orders data file: "; // can go into a catalogue
}

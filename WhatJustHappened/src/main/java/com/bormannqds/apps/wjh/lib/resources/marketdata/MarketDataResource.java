package com.bormannqds.apps.wjh.lib.resources.marketdata;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;

import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.resources.XmlResource;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesInputResource;
import org.apache.commons.csv.CSVFormat;

import java.net.URL;
import java.util.Date;

/**
 *  Provides sequential access to MD files at the locatorBase (MD directory+ticker).
 *
 *  The individual collection members are identified by a Date key and the corresponding file names are derived
 *  as follows:
 *  	path name = MD directory+ticker+date (YYYY-MM-DD)+suffix
 *  where
 *  	suffix = "_L1BOOK.csv"|"_TRADE.csv"
 */
public class MarketDataResource	extends XmlResource {

	public MarketDataResource(final URL locatorBase, final String ticker) {
		super("Market Data", locatorBase);
		this.ticker = ticker;
		this.csvFormat = CSVFormat.DEFAULT.withRecordSeparator(ApplicationContext.getInstance().getConfigurationResource().getDefaultCsvRecordSeparator())
											.withHeader();
	}

	public String getTicker() {
		return ticker;
	}

	public TimeSeriesInputResource<Quote> getQuoteStream(final Date date, final L1QuoteFiltering quoteFiltering) throws ResourceNotFoundException {
		return new CSVParserQuoteStream(getLocator(), ticker, date, quoteFiltering, csvFormat);
	}

	public TimeSeriesInputResource<Trade> getTradeStream(final Date date) throws ResourceNotFoundException {
		return new CSVParserTradeStream(getLocator(), ticker, date, csvFormat);
	}

	// -------- Protected ---------

	@Override
	protected void reset() throws ResourceIOException {
		// Currently nothing to do
	}

	// -------- Private ----------

	private final String ticker;
	private final CSVFormat csvFormat;
}

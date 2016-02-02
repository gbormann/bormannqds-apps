package com.bormannqds.apps.wjh.lib.resources.tradingdata;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;

import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.resources.XmlResource;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesInputResource;
import org.apache.commons.csv.CSVFormat;

import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 *  Provides sequential access to trading data (TD) files at the locatorBase (TD dir+strategy+date).
 *  
 *  The individual collection members are identified by a Date key and the corresponding file names are derived
 *  as follows:
 *  	path name = TD directory+strategy+date (YYYY-MM-DD)+suffix
 *  where
 *  	suffix = "_ORDERS.csv"|"_FILLS.csv"
 */
public class TradingDataResource extends XmlResource {

	public TradingDataResource(final URL locatorBase, final String strategy) {
		super("Trading Data", locatorBase);
		this.strategy = strategy;
		this.csvFormat = CSVFormat.DEFAULT.withRecordSeparator(ApplicationContext.getInstance().getConfigurationResource().getDefaultCsvRecordSeparator())
											.withHeader();
	}

	public TimeSeriesInputResource<Order> getOrderStream(final Date date) throws ResourceNotFoundException {
		TimeSeriesInputResource<Order> orderStream = orderStreamCache.get(date);
		if (orderStream == null) {
			orderStream = new CSVParserOrderStream(getLocator(), strategy, date, csvFormat);
			orderStreamCache.put(date, orderStream);
		}
		return orderStream;
	}

	public TimeSeriesInputResource<Fill> getFillStream(final Date date) throws ResourceNotFoundException {
		TimeSeriesInputResource<Fill> fillStream = fillStreamCache.get(date);
		if (fillStream == null) {
			fillStream = new CSVParserFillStream(getLocator(), strategy, date, csvFormat);
			fillStreamCache.put(date, fillStream);
		}
		return fillStream;
	}

	public void dispose() throws ResourceIOException {
		for (TimeSeriesInputResource<Order> orderStream: orderStreamCache.values()) {
			orderStream.close();
		}
		orderStreamCache.clear(); // clear the cache
		for (TimeSeriesInputResource<Fill> fillStream: fillStreamCache.values()) {
			fillStream.close();
		}
		fillStreamCache.clear(); // clear the cache
	}

	// -------- Protected ---------

	@Override
	protected void reset() throws ResourceIOException {
		dispose();
	}

	// -------- Private ----------

	private final String strategy;
	private final CSVFormat csvFormat;
	private final Map<Date, TimeSeriesInputResource<Order>> orderStreamCache = new TreeMap<Date, TimeSeriesInputResource<Order>>();
	private final Map<Date, TimeSeriesInputResource<Fill>> fillStreamCache = new TreeMap<Date, TimeSeriesInputResource<Fill>>();
}

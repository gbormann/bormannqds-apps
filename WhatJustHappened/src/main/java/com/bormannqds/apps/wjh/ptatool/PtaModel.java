package com.bormannqds.apps.wjh.ptatool;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.ptadata.*;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.OrderSide;
import com.bormannqds.apps.wjh.lib.timeseries.TimeSeriesKeys;

import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.Bracket;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesInputResource;
import com.bormannqds.lib.utils.chrono.CalendarUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PtaModel {
	public void createChartModels(final PtaDataDescriptor ptaDataDescriptor) throws ResourceNotFoundException, ResourceIOException {
		legCollectionsMap.clear();
	
		Date ts0 = null;
		try {
			ts0 = tsFormat.parse(ptaDataDescriptor.getDateString() + " 00:00:00.000");
		}
		catch (ParseException pe) {
			LOGGER.error("Date on orders file is not of the expected format!", pe);
			// we'll catch a resource error later...
		}

		PtaDataResource pdResource = ApplicationContext.getInstance().getPtaDataResource(ptaDataDescriptor.getDateString(),
																							ptaDataDescriptor.getStrategy());
		final TimeSeriesInputResource<PtaItem> ptaItemStream = pdResource.getPtaItemStream(ptaDataDescriptor.getParentOrderId());
		ptaItemStream.open();

		Bracket<PtaItem> ptaItemBracket = ptaItemStream.getBracket(ts0);
		for (; ptaItemBracket.getLater() != null; ptaItemBracket = ptaItemStream.propagateBracket(ptaItemBracket)) {
			final Date tStart = ptaItemBracket.getEarlier() == null ? ts0 : ptaItemBracket.getEarlier().getTimestamp();
			processPtaItem(tStart, ptaItemBracket.getLater());
		}

		ptaItemStream.close();		
	}

	public Map<String, Map<TimeSeriesCollectionKeys, TimeSeriesCollection>> getChartModels() {
		return legCollectionsMap;
	}

	public Map<String, OrderSide> getLegSides() {
		return basketLegDirectionMap;
	}

	// -------- Private ----------

	private Map<TimeSeriesCollectionKeys, TimeSeriesCollection> getLegCollections(final String ticker) {
		Map<TimeSeriesCollectionKeys, TimeSeriesCollection> legCollections = legCollectionsMap.get(ticker);
		if (legCollections == null) {
			legCollections = new TreeMap<TimeSeriesCollectionKeys, TimeSeriesCollection>();
			TimeSeriesKeys modelVarToPlot = ApplicationContext.getInstance().getConfigurationResource().getModelVarToPlot();
			TimeSeriesCollection quotes = new TimeSeriesCollection();
			TimeSeriesCollection vars = new TimeSeriesCollection();
			TimeSeriesCollection trades = new TimeSeriesCollection();
			TimeSeriesCollection orders = new TimeSeriesCollection();
			TimeSeriesCollection events = new TimeSeriesCollection();
			TimeSeriesCollection fills = new TimeSeriesCollection();
			quotes.addSeries(new TimeSeries(TimeSeriesKeys.Bids));
			quotes.addSeries(new TimeSeries(TimeSeriesKeys.Asks));
			vars.addSeries(new TimeSeries(modelVarToPlot));
			trades.addSeries(new TimeSeries(TimeSeriesKeys.Trades));
			orders.addSeries(new TimeSeries(TimeSeriesKeys.Orders));
			events.addSeries(new TimeSeries(TimeSeriesKeys.Events));
			fills.addSeries(new TimeSeries(TimeSeriesKeys.Fills));
			legCollections.put(TimeSeriesCollectionKeys.Quotes, quotes);
			legCollections.put(TimeSeriesCollectionKeys.ModVars, vars);
			legCollections.put(TimeSeriesCollectionKeys.Trades, trades);
			legCollections.put(TimeSeriesCollectionKeys.Orders, orders);
			legCollections.put(TimeSeriesCollectionKeys.Events, events);
			legCollections.put(TimeSeriesCollectionKeys.Fills, fills);
			legCollectionsMap.put(ticker, legCollections);
		}
		return legCollections;
	}

	private void registerOrderDirection(final String ticker, int size) {
		if (basketLegDirectionMap.containsKey(ticker)) return;

		basketLegDirectionMap.put(ticker, size > 0 ? OrderSide.Buy : OrderSide.Sell);
	}
	
	private void processPtaItem(final Date tStart, final PtaItem ptaItem) {
		Map<TimeSeriesCollectionKeys, TimeSeriesCollection> legCollections = getLegCollections(ptaItem.getTicker());
		TimeSeriesKeys modelVarToPlot = ApplicationContext.getInstance().getConfigurationResource().getModelVarToPlot();
		switch (ptaItem.getEvent()) {
		case 'Q':
			SummaryQuote quoteItem = (SummaryQuote)ptaItem;
			TimeSeriesCollection quotes = legCollections.get(TimeSeriesCollectionKeys.Quotes);
			TimeSeriesCollection vars = legCollections.get(TimeSeriesCollectionKeys.ModVars);
			TimeSeries bids = quotes.getSeries(TimeSeriesKeys.Bids.getNdx());
			TimeSeries asks = quotes.getSeries(TimeSeriesKeys.Asks.getNdx());
			TimeSeries modelVarvals = vars.getSeries(modelVarToPlot.getNdx());
			Millisecond timeStamp = new Millisecond(quoteItem.getTimestamp());
			bids.add(timeStamp, quoteItem.getBid());
			asks.add(timeStamp, quoteItem.getAsk());

			// -----	Common intermediates    -------
			double v = quoteItem.getBidSize() + quoteItem.getAskSize();
//			double dv = (quoteItem.getBidSize() - quoteItem.getAskSize()) / v;
			switch (modelVarToPlot) {
			case ExpPs:
				// -----	Expected novation price from book imbalance    -------
//				double s = quoteItem.getAsk() - quoteItem.getBid();
//				double m = (quoteItem.getAsk() + quoteItem.getBid()) / 2;
				double expP = (1.5 - quoteItem.getBidSize() / (v / 2.0)) * quoteItem.getBid()
								+ (1.5 - quoteItem.getAskSize() / (v / 2.0)) * quoteItem.getAsk();
//				double expP_mp = m + dv * s; // equivalent expression
				modelVarvals.add(timeStamp, expP);
				break;
			case PUps:

				// -----    Quote Novation probability from book imbalance   -----		
//				double p_d = quoteItem.getAskSize() / v;
				double p_u = quoteItem.getBidSize() / v;
				modelVarvals.add(timeStamp, p_u);
				break;
			default: // should never get here
			}
			break;
		case 'T':
			SummaryTrade tradeItem = (SummaryTrade)ptaItem;
			TimeSeries trades = legCollections.get(TimeSeriesCollectionKeys.Trades).getSeries(0);
			trades.add(new Millisecond(tradeItem.getTimestamp()), tradeItem.getPrice());
			break;
		case 'O':
			SummaryOrder orderItem = (SummaryOrder)ptaItem;
			TimeSeries orders = legCollections.get(TimeSeriesCollectionKeys.Orders).getSeries(0);
			TimeSeries events = legCollections.get(TimeSeriesCollectionKeys.Events).getSeries(0);
			orders.add(new Millisecond(orderItem.getTimestamp()), orderItem.getPrice());
			events.add(new Millisecond(orderItem.getTimestamp()), orderItem.getPrice()); // redundant but quickest way to get the legend right
			registerOrderDirection(orderItem.getTicker(), orderItem.getSize());
			break;
		case 'F':
			SummaryFill fillItem = (SummaryFill)ptaItem;
			TimeSeries fills = legCollections.get(TimeSeriesCollectionKeys.Fills).getSeries(0);
			fills.add(new Millisecond(fillItem.getTimestamp()), fillItem.getPrice());
			break;
		}
	}

	private static final Logger LOGGER = LogManager.getLogger(PtaModel.class);

	private final DateFormat tsFormat = new SimpleDateFormat(CalendarUtils.TIMESTAMP_FORMAT); // let's assume for now we're single-threaded
	private final Map<String, Map<TimeSeriesCollectionKeys, TimeSeriesCollection>> legCollectionsMap = new HashMap<String, Map<TimeSeriesCollectionKeys, TimeSeriesCollection>>();
	private final Map<String, OrderSide> basketLegDirectionMap = new HashMap<String, OrderSide>();
}

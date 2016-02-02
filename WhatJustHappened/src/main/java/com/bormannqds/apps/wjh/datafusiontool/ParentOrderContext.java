package com.bormannqds.apps.wjh.datafusiontool;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;
import com.bormannqds.apps.wjh.lib.resources.marketdata.L1QuoteFiltering;
import com.bormannqds.apps.wjh.lib.resources.marketdata.Quote;
import com.bormannqds.apps.wjh.lib.resources.marketdata.Trade;
import com.bormannqds.apps.wjh.lib.resources.ptadata.*;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.Fill;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.LegTag;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.Order;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.ParentOrder;

import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.Bracket;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesInputResource;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesOutputResource;
import org.apache.commons.csv.CSVFormat;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ParentOrderContext {
	public ParentOrderContext(final URL baseLocator,
								final Order protoOrder,
								final L1QuoteFiltering quoteFiltering) throws IOException, ResourceNotFoundException {
		this.quoteFiltering = quoteFiltering;
		parentOrder = new ParentOrder(protoOrder);
		String parentOrderId = protoOrder.getParentOrderId();

		ptaItemSink = new CSVPrinterPtaItemStream(
				DataAccessUtils.createPtaLocator(baseLocator, parentOrderId),
				CSVFormat.DEFAULT.withRecordSeparator(ApplicationContext.getInstance().getConfigurationResource().getDefaultCsvRecordSeparator())
												 );
		ptaItemSink.open();
	}

	public ParentOrder getParentOrder() {
		return parentOrder;
	}

	public void processOrderEvent(final Date date, final Order order) throws IOException, ResourceNotFoundException {
		long lifeTime = 0;
		parentOrder.setLeg(order.getLegTag(), order.getTicker());
		switch (order.getOrderState()) {
		case PENDING_NEW:
			newOrderTimestampCache.put(order.getOrderId(), order.getTimestamp().getTime());
			break;
		case PENDING_AMEND:
		case PENDING_CANCEL:
		case NEW_ORDER_SINGLE:
		case AMENDED:
			lifeTime = order.getTimestamp().getTime() - newOrderTimestampCache.get(order.getOrderId());
			break;
		case CANCELED:
		case FILLED:
		case REJECTED:
			lifeTime = order.getTimestamp().getTime() - newOrderTimestampCache.get(order.getOrderId());
			newOrderTimestampCache.remove(order.getOrderId());
			break;
		default: // unknown order event -> should not happen!
			return;
		}
		if (lifeTime > 0) {
			TimeSeriesInputResource<Quote> quoteStream = getLegQuoteStream(date, order);
			Bracket<Quote> quoteBracket = getLegQuoteBracket(date, order);
			while(quoteBracket != null && !quoteBracket.isInsideBracket(order.getTimestamp())) {
				quoteBracket = quoteStream.propagateBracket(quoteBracket);
				if (quoteBracket.getEarlier() != null) {
					ptaData.add(generateSummaryQuote(order.getTicker(), quoteBracket.getEarlier()));
				}
			}
			TimeSeriesInputResource<Trade> tradeStream = getLegTradeStream(date, order);
			Bracket<Trade> tradeBracket = getLegTradeBracket(date, order);
			while(tradeBracket != null && !tradeBracket.isInsideBracket(order.getTimestamp())) {
				tradeBracket = tradeStream.propagateBracket(tradeBracket);
				if (tradeBracket.getEarlier() != null) {
					ptaData.add(generateSummaryTrade(order.getTicker(), tradeBracket.getEarlier()));
				}
			}
		}
		else {
			Bracket<Quote> quoteBracket = findLegQuoteBracket(date, order);
			if (quoteBracket != null && quoteBracket.getEarlier() != null) {
				ptaData.add(generateSummaryQuote(order.getTicker(), quoteBracket.getEarlier()));
			}
			Bracket<Trade> tradeBracket = findLegTradeBracket(date, order);
			if (tradeBracket != null && tradeBracket.getEarlier() != null) {
				ptaData.add(generateSummaryTrade(order.getTicker(), tradeBracket.getEarlier()));
			}
		}
		ptaData.add(generateSummaryOrder(order, lifeTime));
	}

	public void processFillEvent(final Date date, final Fill fill) {
		ptaData.add(generateSummaryFill(fill));
	}

	public void exportPtaData() throws ResourceIOException {
		for (PtaItem ptaItem: ptaData) { // sorted by virtue of ptaData being a TreeSet
			ptaItemSink.putSample(ptaItem);
		}
		ptaItemSink.setDirty();
	}

	public void closeAll() throws ResourceIOException {
		for (TimeSeriesInputResource<Quote> quoteStream: legQuoteStreamCache.values()) {
			quoteStream.close();
		}		
		for (TimeSeriesInputResource<Trade> tradeStream: legTradeStreamCache.values()) {
			tradeStream.close();
		}
	}

	public void dispose() throws ResourceIOException {
		closeAll();
		ptaItemSink.close();
		newOrderTimestampCache.clear();
		ptaData.clear();
		legQuoteStreamCache.clear(); // clear the cache
		legTradeStreamCache.clear(); // clear the cache
		legQuoteBracketCache.clear();
		legTradeBracketCache.clear();
	}

	// -------- Private ----------

	private TimeSeriesInputResource<Quote> getLegQuoteStream(final Date date, final Order order) throws ResourceNotFoundException, ResourceIOException {
		TimeSeriesInputResource<Quote> quoteStream = legQuoteStreamCache.get(order.getLegTag());
		if (quoteStream == null) {
			quoteStream = ApplicationContext.getInstance().getMarketDataResource(order.getTicker()).getQuoteStream(date, quoteFiltering);
			legQuoteStreamCache.put(order.getLegTag(), quoteStream);
		}
		if (!quoteStream.isOpen()) {
			quoteStream.open();
		}
		return quoteStream;
	}

	private TimeSeriesInputResource<Trade> getLegTradeStream(final Date date, final Order order) throws ResourceNotFoundException, ResourceIOException {
		TimeSeriesInputResource<Trade> tradeStream = legTradeStreamCache.get(order.getLegTag());
		if (tradeStream == null) {
			tradeStream = ApplicationContext.getInstance().getMarketDataResource(order.getTicker()).getTradeStream(date);
			legTradeStreamCache.put(order.getLegTag(), tradeStream);
		}
		if (!tradeStream.isOpen()) {
			tradeStream.open();
		}
		return tradeStream;
	}

	private Bracket<Quote> getLegQuoteBracket(final Date date, final Order order) throws ResourceNotFoundException, ResourceIOException {
		Bracket<Quote> quoteBracket = legQuoteBracketCache.get(order.getLegTag());
		if (quoteBracket == null) {
			quoteBracket = getLegQuoteStream(date, order).getBracket(order.getTimestamp());
			legQuoteBracketCache.put(order.getLegTag(), quoteBracket);
		}
		return quoteBracket;
	}

	// get initial trade bracket and cache on first retrieval
	private Bracket<Trade> getLegTradeBracket(final Date date, final Order order) throws ResourceNotFoundException, ResourceIOException {
		Bracket<Trade> tradeBracket = legTradeBracketCache.get(order.getLegTag());
		if (tradeBracket == null) {
			tradeBracket = getLegTradeStream(date, order).getBracket(order.getTimestamp());
			legTradeBracketCache.put(order.getLegTag(), tradeBracket);
		}
		return tradeBracket;
	}

	private Bracket<Quote> findLegQuoteBracket(final Date date, final Order order) throws ResourceNotFoundException, ResourceIOException {
		Bracket<Quote> quoteBracket = getLegQuoteBracket(date, order);

		if (!quoteBracket.isInsideBracket(order.getTimestamp())) {
			getLegQuoteStream(date, order).propagateBracket(quoteBracket, order.getTimestamp());
		}

		return quoteBracket;
	}

	// get cached bracket and propagate if needed to bracket order ts
	private Bracket<Trade> findLegTradeBracket(final Date date, final Order order) throws ResourceNotFoundException, ResourceIOException {
		Bracket<Trade> tradeBracket = getLegTradeBracket(date, order);

		if (!tradeBracket.isInsideBracket(order.getTimestamp())) {
			getLegTradeStream(date, order).propagateBracket(tradeBracket, order.getTimestamp());
		}

		return tradeBracket;
	}

	private SummaryTrade generateSummaryTrade(final String ticker, final Trade trade) {
		if (trade == null) {
			return null;
		}
		return new SummaryTrade(trade.getTimestamp(), ticker, trade.getPrice(),	trade.getSize());
	}

	private SummaryQuote generateSummaryQuote(final String ticker, final Quote quote) {
		if (quote == null) {
			return null;
		}
		return new SummaryQuote(quote.getTimestamp(), ticker,
								quote.getAsk(), quote.getBid(),
								quote.getAskSize(), quote.getBidSize());
	}

	private SummaryOrder generateSummaryOrder(final Order order, final long lifeTime) {
		return new SummaryOrder(order.getTimestamp(), order.getTicker(),
								order.getPrice(), order.getSize(),
								order.getOrderId(), order.getOrderState(), lifeTime);
	}

	private SummaryFill generateSummaryFill(final Fill fill) {
		return new SummaryFill(fill.getTimestamp(), fill.getTicker(),
								fill.getPrice(), fill.getSize(),
								fill.getOrderId(), fill.getFillId());
	}

	private final ParentOrder parentOrder;
	private final TimeSeriesOutputResource<PtaItem> ptaItemSink;
	private final Map<Integer, Long> newOrderTimestampCache = new TreeMap<Integer, Long>();
	private final Set<PtaItem> ptaData = new TreeSet<PtaItem>(new PtaItemComparator());
	private final Map<LegTag, TimeSeriesInputResource<Quote>> legQuoteStreamCache = new TreeMap<LegTag, TimeSeriesInputResource<Quote>>();
	private final Map<LegTag, TimeSeriesInputResource<Trade>> legTradeStreamCache = new TreeMap<LegTag, TimeSeriesInputResource<Trade>>();
	private final Map<LegTag, Bracket<Quote>> legQuoteBracketCache= new TreeMap<LegTag, Bracket<Quote>>();
	private final Map<LegTag, Bracket<Trade>> legTradeBracketCache= new TreeMap<LegTag, Bracket<Trade>>();
	private final L1QuoteFiltering quoteFiltering;
}

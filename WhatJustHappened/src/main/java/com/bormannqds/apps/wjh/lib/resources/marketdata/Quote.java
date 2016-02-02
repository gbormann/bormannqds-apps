package com.bormannqds.apps.wjh.lib.resources.marketdata;

import java.util.Date;

import com.bormannqds.lib.dataaccess.timeseries.AbstractTimeSeriesSample;

/** Minimal quote interface. */
public class Quote extends AbstractTimeSeriesSample {
	public Quote(final Date timestamp, double bid, double ask, int bidSize, int askSize) {
		super(timestamp);
		this.bid = bid;
		this.ask = ask;
		this.bidSize = bidSize;
		this.askSize = askSize;
	}

	public double getBid() {
		return bid;
	}

	public double getAsk() {
		return ask;
	}

	public int getBidSize() {
		return bidSize;
	}

	public int getAskSize() {
		return askSize;
		
	}

	@Override
	public String toString() {
		return "Quote [" + super.toString() + ", bid=" + bid + ", ask=" + ask + ", bidSize=" + bidSize
				+ ", askSize=" + askSize + "]";
	}

	// -------- Private ----------

	private final double bid;
	private final double ask;
	private final int bidSize;
	private final int askSize;
}

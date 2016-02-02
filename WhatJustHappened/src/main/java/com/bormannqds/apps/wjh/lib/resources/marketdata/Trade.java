package com.bormannqds.apps.wjh.lib.resources.marketdata;

import com.bormannqds.lib.dataaccess.timeseries.AbstractTimeSeriesSample;

import java.util.Date;

/** Minimal trade interface. */
public class Trade extends AbstractTimeSeriesSample {
	public Trade(final Date timestamp, double price, int size) {
		super(timestamp);
		this.price = price;
		this.size = size;
	}

	public double getPrice() {
		return price;
	}

	public int getSize() {
		return size;
	}

	// -------- Private ----------

	private final double price;
	private final int size;
}

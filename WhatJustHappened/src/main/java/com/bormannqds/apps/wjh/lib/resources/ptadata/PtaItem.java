package com.bormannqds.apps.wjh.lib.resources.ptadata;

import com.bormannqds.lib.dataaccess.timeseries.AbstractTimeSeriesSample;

import java.util.Date;

/** Minimal PtaItem interface. */
public abstract class PtaItem extends AbstractTimeSeriesSample {
	public String getTicker() {
		return ticker;
	}

	public abstract char getEvent();

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("PtaItem [");
		stringBuilder.append(super.toString())
			.append(", ticker=").append(ticker)
			.append(", event=").append(getEvent())
			.append(']');
		return stringBuilder.toString();
	}

	// -------- Protected ----------

	protected PtaItem(final Date timestamp, final String ticker) {
		super(timestamp);
		this.ticker = ticker;
	}

	// -------- Private ----------

	private final String ticker;
}

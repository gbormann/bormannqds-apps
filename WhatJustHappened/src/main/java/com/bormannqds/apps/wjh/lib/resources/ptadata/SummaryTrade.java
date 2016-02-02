package com.bormannqds.apps.wjh.lib.resources.ptadata;

import java.util.Date;

public class SummaryTrade extends PtaItem {

	public SummaryTrade(final Date timestamp, final String ticker, double price, int size) {
		super(timestamp, ticker);
		this.price = price;
		this.size = size;
	}

	@Override
	public char getEvent() {
		return 'T';
	}

	public double getPrice() {
		return price;
	}

	public int getSize() {
		return size;
	}

	// -------- Private ----------

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("SummaryTrade [");
		stringBuilder.append(super.toString())
			.append(", event=").append(getEvent())
			.append(", price=").append(price)
			.append(", size=").append(size)
			.append("]");

		return stringBuilder.toString();
	}

	private final double price;
	private final int size;
}

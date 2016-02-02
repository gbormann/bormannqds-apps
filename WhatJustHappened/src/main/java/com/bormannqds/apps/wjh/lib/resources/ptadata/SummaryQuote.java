package com.bormannqds.apps.wjh.lib.resources.ptadata;

import java.util.Date;

public class SummaryQuote extends PtaItem {

	public SummaryQuote(final Date timestamp, final String ticker, double ask, double bid, int askSize, int bidSize) {
		super(timestamp, ticker);
		this.ask = ask;
		this.bid = bid;
		this.askSize = askSize;
		this.bidSize = bidSize;
	}

	@Override
	public char getEvent() {
		return 'Q';
	}

	public double getAsk() {
		return ask;
	}

	public double getBid() {
		return bid;
	}

	public int getAskSize() {
		return askSize;
	}

	public int getBidSize() {
		return bidSize;
	}

	// -------- Private ----------

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("SummaryQuote [");
		stringBuilder.append(super.toString())
			.append(", event=").append(getEvent())
			.append(", ask=").append(ask)
			.append(", bid=").append(bid)
			.append(", askSize=").append(askSize)
			.append(", bidSize=").append(bidSize)
			.append("]");

		return stringBuilder.toString();
	}

	private final double ask;
	private final double bid;
	private final int askSize;
	private final int bidSize;
}

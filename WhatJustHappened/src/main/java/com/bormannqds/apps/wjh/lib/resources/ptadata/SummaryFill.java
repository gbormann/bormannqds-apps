package com.bormannqds.apps.wjh.lib.resources.ptadata;

import java.util.Date;

public class SummaryFill extends PtaItem {

	public SummaryFill(final Date timestamp,
							final String ticker,
							double price,
							int size,
							int orderId,
							int fillId) {
		super(timestamp, ticker);
		this.price = price;
		this.size = size;
		this.orderId = orderId;
		this.fillId = fillId;
	}

	@Override
	public char getEvent() {
		return 'F';
	}

	public double getPrice() {
		return price;
	}

	public int getSize() {
		return size;
	}

	public int getOrderId() {
		return orderId;
	}

	public int getFillId() {
		return fillId;
	}

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("SummaryFill [");
		stringBuilder.append(super.toString())
			.append(", event=").append(getEvent())
			.append(", price=").append(price)
			.append(", size=").append(size)
			.append(", orderId=").append(orderId)
			.append(", fillId=").append(fillId)
			.append("]");

		return stringBuilder.toString();
	}

	// -------- Private ----------

	private final double price;
	private final int size;
	private final int orderId;
	private final int fillId;
}

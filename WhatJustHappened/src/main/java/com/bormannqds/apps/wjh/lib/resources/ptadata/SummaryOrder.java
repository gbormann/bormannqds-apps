package com.bormannqds.apps.wjh.lib.resources.ptadata;

import java.util.Date;

import com.bormannqds.apps.wjh.lib.resources.tradingdata.OrderState;

public class SummaryOrder extends PtaItem {

	public SummaryOrder(final Date timestamp,
							final String ticker,
							double price,
							int size,
							int orderId,
							final OrderState orderState,
							long lifeTime) {
		super(timestamp, ticker);
		this.price = price;
		this.size = size;
		this.orderId = orderId;
		this.orderState = orderState;
		this.lifeTime = lifeTime;
	}

	@Override
	public char getEvent() {
		return 'O';
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

	public OrderState getOrderState() {
		return orderState;
	}

	public long getLifeTime() {
		return lifeTime;
	}

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("SummaryOrder [");
		stringBuilder.append(super.toString())
			.append(", event=").append(getEvent())
			.append(", price=").append(price)
			.append(", size=").append(size)
			.append(", orderId=").append(orderId)
			.append(", orderState=").append(orderState.name())
			.append(", lifeTime=").append(lifeTime)
			.append("]");

		return stringBuilder.toString();
	}

	// -------- Private ----------

	private final double price;
	private final int size;
	private final int orderId;
	private final OrderState orderState;
	private final long lifeTime;
}

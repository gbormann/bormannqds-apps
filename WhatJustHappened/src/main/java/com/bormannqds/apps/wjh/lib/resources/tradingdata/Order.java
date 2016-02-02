package com.bormannqds.apps.wjh.lib.resources.tradingdata;

import com.bormannqds.lib.dataaccess.timeseries.AbstractTimeSeriesSample;

import java.util.Date;

/** Minimal trade interface. */
/* timestamp: ts,
 * [ts in 1/10 microseconds: atts],
 * BookingData: {book,trader,strategy},
 * Enum altId: altid,
 * parentOrderId: parentid,
 * legTag: tag,
 * ticker: ticker,
 * orderId: orderid,
 * [unique ID?: sid],
 * limitPrice: limitprice,
 * size: signedsize,
 * Enum state: requested, submitted, amendrequested, amendconfirmed, cancelrequest, cancelled, filled, rejected
 */
public class Order extends AbstractTimeSeriesSample {

	public Order(final Date timestamp,
					final BookingData bookingData,
					final AltId altId,
					final String parentOrderId,
					final LegTag legTag,
					final String ticker,
					final OrderState orderState,
					int orderId,
					int size,
					double price) {
		super(timestamp);
		this.bookingData = bookingData;
		this.altId = altId;
		this.parentOrderId = parentOrderId;
		this.legTag = legTag;
		this.ticker = ticker;
		this.orderState = orderState;
		this.orderId = orderId;
		this.size = size;
		this.price = price;
	}

	public BookingData getBookingData() {
		return bookingData;
	}

	public AltId getAltId() {
		return altId;
	}

	public String getParentOrderId() {
		return parentOrderId;
	}

	public LegTag getLegTag() {
		return legTag;
	}

	public String getTicker() {
		return ticker;
	}

	public OrderState getOrderState() {
		return orderState;
	}

	public int getOrderId() {
		return orderId;
	}

	public int getSize() {
		return size;
	}

	public double getPrice() {
		return price;
	}

	@Override
	public String toString() {
		return "Order [" + super.toString() + ", bookingData=" + bookingData + ", altId=" + altId
				+ ", parentOrderId=" + parentOrderId + ", legTag=" + legTag
				+ ", ticker=" + ticker + ", orderState=" + orderState
				+ ", orderId=" + orderId + ", size=" + size + ", price="
				+ price + "]";
	}

	// -------- Private ----------

	private final BookingData bookingData;
	private final AltId altId;
	private final String parentOrderId;
	private final LegTag legTag;
	private final String ticker;
	private final OrderState orderState;
	private final int orderId;
	private final int size;
	private final double price; // limit price or execution price (if orderState=FILLED)
}

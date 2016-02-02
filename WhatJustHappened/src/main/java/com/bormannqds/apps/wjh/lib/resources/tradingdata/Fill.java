package com.bormannqds.apps.wjh.lib.resources.tradingdata;

import com.bormannqds.lib.dataaccess.timeseries.AbstractTimeSeriesSample;

import java.util.Date;

/** Minimal trade interface. */
/* ts,
 * strategy,
 * parentid,
 * ticker,
 * orderid,
 * fid,
 * fillsize,
 * price,
 * [targetprice,spreadprice]
 */
public class Fill extends AbstractTimeSeriesSample {

	public Fill(final Date timestamp,
					final BookingData bookingData,
					final String parentOrderId,
					final String ticker,
					int orderId,
					int fillId,
					int size,
					double price) {
		super(timestamp);
		this.bookingData = bookingData;
		this.parentOrderId = parentOrderId;
		this.ticker = ticker;
		this.orderId = orderId;
		this.fillId = fillId;
		this.size = size;
		this.price = price;
	}

	public BookingData getBookingData() {
		return bookingData;
	}

	public String getParentOrderId() {
		return parentOrderId;
	}

	public String getTicker() {
		return ticker;
	}

	public int getOrderId() {
		return orderId;
	}

	public int getFillId() {
		return fillId;
	}

	public int getSize() {
		return size;
	}

	public double getPrice() {
		return price;
	}

	// -------- Private ----------

	private final BookingData bookingData;
	private final String parentOrderId;
	private final String ticker;
	private final int orderId;
	private final int fillId;
	private final int size;
	private final double price; // limit price or execution price (if orderState=FILLED)
}

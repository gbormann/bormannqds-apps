package com.bormannqds.apps.wjh.lib.resources.tradingdata;

/**
 * This is not a container of child orders!
 * @author guy
 *
 */
public class ParentOrder {

	public ParentOrder(final Order order) {
		protoOrder = order;
	}

	public BookingData getBookingData() {
		return protoOrder.getBookingData();
	}

	public String getOrderId() {
		return protoOrder.getParentOrderId();
	}

	public AltId getAltId() {
		return protoOrder.getAltId();
	}

	public String[] getLegs() {
		return legs;
	}

	public void setLeg(final LegTag legTag, final String ticker) {
		if (legs[legTag.ordinal()] == null) {
			legs[legTag.ordinal()] = ticker;
		}
	}
/*
	@Override
	public String toString() {
		return getOrderId();
	}
*/
	// -------- Private ----------

	private final Order protoOrder;
	private final String[] legs = new String[LegTag.MAX_NR_LEGS.ordinal()];
}
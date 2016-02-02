package com.bormannqds.apps.wjh.lib.resources.tradingdata;

public enum OrderState {
	PENDING_NEW, // requested
	NEW_ORDER_SINGLE, // submitted
	PENDING_AMEND, // amendrequested
	AMENDED, // amendconfirmed
	PENDING_CANCEL, // cancelrequest
	CANCELED, // canceled
	FILLED, // filled
	REJECTED // rejected
}

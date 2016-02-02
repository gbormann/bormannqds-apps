package com.bormannqds.apps.wjh.ptatool;

public class PtaDataDescriptor {
	public String getDateString() {
		return dateString;
	}

	public String getStrategy() {
		return strategy;
	}

	public String getParentOrderId() {
		return parentOrderId;
	}

	public void setDateString(String dateString) {
		this.dateString = dateString;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public void setParentOrderId(String parentOrderId) {
		this.parentOrderId = parentOrderId;
	}

	public void reset() {
		dateString = null;
		strategy = null;
		parentOrderId = null;
	}

	// -------- Private ----------

	private String dateString;
	private String strategy;
	private String parentOrderId;
}

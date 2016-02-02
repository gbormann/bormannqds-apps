package com.bormannqds.apps.wjh.lib.resources.marketdata;

public class QuotePriceFieldIndices {
	public void setFieldIndices(int bidFieldNdx, int askFieldNdx) {
		this.bidFieldNdx = bidFieldNdx;
		this.askFieldNdx = askFieldNdx;
	}

	public int getBidFieldNdx() {
		return bidFieldNdx;
	}

	public int getAskFieldNdx() {
		return askFieldNdx;
	}

	// -------- Private ----------

	private int bidFieldNdx;
	private int askFieldNdx;
}

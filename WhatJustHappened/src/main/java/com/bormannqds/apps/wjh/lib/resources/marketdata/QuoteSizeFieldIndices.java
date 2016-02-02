package com.bormannqds.apps.wjh.lib.resources.marketdata;

public class QuoteSizeFieldIndices {
	public void setFieldIndices(int bidSizeFieldNdx, int askSizeFieldNdx) {
		this.bidSizeFieldNdx = bidSizeFieldNdx;
		this.askSizeFieldNdx = askSizeFieldNdx;
	}

	public int getBidSizeFieldNdx() {
		return bidSizeFieldNdx;
	}

	public int getAskSizeFieldNdx() {
		return askSizeFieldNdx;
	}

	// -------- Private ----------

	private int bidSizeFieldNdx;
	private int askSizeFieldNdx;
}

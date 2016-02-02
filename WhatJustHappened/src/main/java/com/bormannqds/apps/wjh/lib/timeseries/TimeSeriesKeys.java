package com.bormannqds.apps.wjh.lib.timeseries;

public enum TimeSeriesKeys {
	Bids(0), Asks(1), ExpPs(0), PUps(0), Trades(0), Orders(0), Events(0), Fills(0);

	public int getNdx() {
		return ndx;
	}

	private TimeSeriesKeys(int ndx) {
		this.ndx = ndx;
	}
	
	private int ndx;
}

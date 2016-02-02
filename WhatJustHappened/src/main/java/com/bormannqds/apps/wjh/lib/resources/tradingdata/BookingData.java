package com.bormannqds.apps.wjh.lib.resources.tradingdata;

public final class BookingData {
	public BookingData(final String book, final String trader, final String strategy) {
		this.book = book;
		this.trader = trader;
		this.strategy = strategy;

		if (book != null) {
			final StringBuilder tagBuilder = new StringBuilder(book);
			tagBuilder.append('_').append(trader);
			this.bookingTag = tagBuilder.toString(); 			
		}
		else {
			this.bookingTag = null;
		}
	}

	public String getBook() {
		return book;
	}
	public String getTrader() {
		return trader;
	}
	public String getStrategy() {
		return strategy;
	}

	public String getBookingTag() {
		return bookingTag;
	}

	@Override
	public String toString() {
		return "BookingData [book=" + book + ", trader=" + trader
				+ ", strategy=" + strategy + "]";
	}

	private final String book;	
	private final String trader;
	private final String bookingTag;
	private final String strategy;
}

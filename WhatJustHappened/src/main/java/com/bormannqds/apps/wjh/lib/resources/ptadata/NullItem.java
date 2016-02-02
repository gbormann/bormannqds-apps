package com.bormannqds.apps.wjh.lib.resources.ptadata;

import java.util.Date;

/**
 * PTA files are generated. This PtaItem type is used to represent entries with bogus event codes
 * due to unreliable field values. In most cases this obviates null pointer checks or the use of an intrusive
 * exception framework for what in some cases might be not so exceptional occurrences. When building charts these
 * samples points can be skipped. Hiatuses in a graph would then suggest something is wrong with the data.
 * 
 * @author guy
 *
 */
public class NullItem extends PtaItem {

	public NullItem(final Date timestamp) {
		super(timestamp, NULLITEM_TICKER_NAME);
	}

	@Override
	public char getEvent() {
		return 'U';
	}

	// -------- Private ----------

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("NullItem [");
		stringBuilder.append(super.toString())
			.append(", event=").append(getEvent())
			.append("]");

		return stringBuilder.toString();
	}

	private static final String NULLITEM_TICKER_NAME = "NULL";
}

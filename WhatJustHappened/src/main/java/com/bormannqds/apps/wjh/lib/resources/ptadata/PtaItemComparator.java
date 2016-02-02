package com.bormannqds.apps.wjh.lib.resources.ptadata;

import java.util.Comparator;

public class PtaItemComparator implements Comparator<PtaItem> {

	/**
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 */
	@Override
	public int compare(PtaItem ptaItem1, PtaItem ptaItem2) {
		return ptaItem1.getTimestamp().before(ptaItem2.getTimestamp()) ?
				-1 : (ptaItem1.getTimestamp().after(ptaItem2.getTimestamp()) ?
						1 : (ptaItem1.getEvent() < ptaItem2.getEvent() ?
								-1 : (ptaItem1.getEvent() > ptaItem2.getEvent() ? 1 : 0)));
	}

}

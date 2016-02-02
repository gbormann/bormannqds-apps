package com.bormannqds.apps.wjh.lib.resources.marketdata;

import com.bormannqds.lib.dataaccess.timeseries.CSV.CsvRecordFilter;
import org.apache.commons.csv.CSVRecord;

public class PriceTransitionFilter extends CsvRecordFilter {
	public PriceTransitionFilter(QuotePriceFieldIndices priceFieldNdcs) {
		this.priceFieldNdcs = priceFieldNdcs;
	}

	@Override
	public boolean accept(CSVRecord record) {
		boolean hasPassed = false;
		if (record == null) {
			return false;
		}
		if (prevRecord == null) {
			hasPassed = true;
		}
		else {
			hasPassed = !(prevRecord.get(priceFieldNdcs.getBidFieldNdx()).equals(record.get(priceFieldNdcs.getBidFieldNdx()))
						&& prevRecord.get(priceFieldNdcs.getAskFieldNdx()).equals(record.get(priceFieldNdcs.getAskFieldNdx())));
		}

		if (hasPassed) {
			prevRecord = record;
		}

		return hasPassed;
	}

	// -------- Private ----------

	private final QuotePriceFieldIndices priceFieldNdcs;
}

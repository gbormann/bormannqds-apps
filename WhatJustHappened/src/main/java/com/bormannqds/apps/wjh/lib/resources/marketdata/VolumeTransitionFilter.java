package com.bormannqds.apps.wjh.lib.resources.marketdata;

import com.bormannqds.lib.dataaccess.timeseries.CSV.CsvRecordFilter;
import org.apache.commons.csv.CSVRecord;

public class VolumeTransitionFilter extends CsvRecordFilter {
	public VolumeTransitionFilter(final QuotePriceFieldIndices priceFieldNdcs, final QuoteSizeFieldIndices sizeFieldNdcs) {
		super();
		this.priceFieldNdcs = priceFieldNdcs;
		this.sizeFieldNdcs = sizeFieldNdcs;
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
			// NOTE: Order is important, making use of short-circuit evaluation of logical expressions,
			// i.e. only check volume difference on equal prices!
			hasPassed = !(prevRecord.get(priceFieldNdcs.getBidFieldNdx()).equals(record.get(priceFieldNdcs.getBidFieldNdx()))
							&& prevRecord.get(sizeFieldNdcs.getBidSizeFieldNdx())
											.equals(record.get(sizeFieldNdcs.getBidSizeFieldNdx())))
						|| !(prevRecord.get(priceFieldNdcs.getAskFieldNdx()).equals(record.get(priceFieldNdcs.getAskFieldNdx()))
							&& prevRecord.get(sizeFieldNdcs.getAskSizeFieldNdx())
											.equals(record.get(sizeFieldNdcs.getAskSizeFieldNdx())));
		}

		if (hasPassed) {
			prevRecord = record;
		}

		return hasPassed;
	}

	// -------- Private ----------

	private final QuotePriceFieldIndices priceFieldNdcs;
	private final QuoteSizeFieldIndices sizeFieldNdcs;
}

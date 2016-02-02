package com.bormannqds.apps.wjh.lib.resources.marketdata;

import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;

import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.CSV.AbstractTimeSeriesCSVParserStream;
import com.bormannqds.lib.dataaccess.timeseries.CSV.NullRecordFilter;
import com.bormannqds.lib.dataaccess.timeseries.Filter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.net.URL;
import java.util.Date;

class CSVParserQuoteStream extends AbstractTimeSeriesCSVParserStream<Quote> {

	public CSVParserQuoteStream(
			final URL baseLocator,
			final String ticker,
			final Date date,
			final L1QuoteFiltering quoteFiltering,
			final CSVFormat csvFormat) throws ResourceNotFoundException {
		super(DataAccessUtils.createQuotesLocator(baseLocator, ticker, date), csvFormat);
		switch (quoteFiltering) {
		case V_TRANSITIONS:
			recordFilter = new VolumeTransitionFilter(priceFieldIndices, sizeFieldIndices);
			break;
		case P_TRANSITIONS:
			recordFilter = new PriceTransitionFilter(priceFieldIndices);
			break;
		case NO_FILTERING:
		default:
			recordFilter = new NullRecordFilter();
			break;
		}
	}

	// -------- Protected ----------

	@Override
	protected Filter<CSVRecord> getRecordFilter() {
		return recordFilter;
	}

	@Override
	protected Quote createSample(final CSVRecord record) {
		return new Quote(parseTimestampField(record),
							parseDoubleField(BID_FIELDNAME, record.get(priceFieldIndices.getBidFieldNdx())),
							parseDoubleField(ASK_FIELDNAME, record.get(priceFieldIndices.getAskFieldNdx())),
							parseIntField(BIDSIZE_FIELDNAME, record.get(sizeFieldIndices.getBidSizeFieldNdx())),
							parseIntField(ASKSIZE_FIELDNAME, record.get(sizeFieldIndices.getAskSizeFieldNdx())));
	}

	@Override
	protected void initialiseSpecificFieldIndices() {
		priceFieldIndices.setFieldIndices(getHeaderMap().get(BID_FIELDNAME), getHeaderMap().get(ASK_FIELDNAME));
		sizeFieldIndices.setFieldIndices(getHeaderMap().get(BIDSIZE_FIELDNAME), getHeaderMap().get(ASKSIZE_FIELDNAME));
	}

	// -------- Private ----------

	private static final String BID_FIELDNAME = "bidprice";
	private static final String ASK_FIELDNAME = "askprice";
	private static final String BIDSIZE_FIELDNAME = "bidsize";
	private static final String ASKSIZE_FIELDNAME = "asksize";

	private final Filter<CSVRecord> recordFilter;
	private final QuotePriceFieldIndices priceFieldIndices = new QuotePriceFieldIndices();
	private final QuoteSizeFieldIndices sizeFieldIndices = new QuoteSizeFieldIndices();
}

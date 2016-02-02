package com.bormannqds.apps.wjh.lib.resources.marketdata;

import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;
import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.CSV.AbstractTimeSeriesCSVParserStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.net.URL;
import java.util.Date;

class CSVParserTradeStream extends AbstractTimeSeriesCSVParserStream<Trade> {

	public CSVParserTradeStream(final URL baseLocator, final String ticker, final Date date, final CSVFormat csvFormat) throws ResourceNotFoundException {
		super(DataAccessUtils.createTradesLocator(baseLocator, ticker, date), csvFormat);
	}

	// -------- Protected ----------

	@Override
	protected Trade createSample(final CSVRecord record) {
		return new Trade(parseTimestampField(record),
							parseDoubleField(PRICE_FIELDNAME, record.get(priceFieldNdx)),
							parseIntField(SIZE_FIELDNAME, record.get(sizeFieldNdx)));
	}

	@Override
	protected void initialiseSpecificFieldIndices() {
		priceFieldNdx = getHeaderMap().get(PRICE_FIELDNAME);
		sizeFieldNdx = getHeaderMap().get(SIZE_FIELDNAME);
	}

	// -------- Private ----------

	private static final String PRICE_FIELDNAME = "price";
	private static final String SIZE_FIELDNAME = "size";

	private int priceFieldNdx;
	private int sizeFieldNdx;
}

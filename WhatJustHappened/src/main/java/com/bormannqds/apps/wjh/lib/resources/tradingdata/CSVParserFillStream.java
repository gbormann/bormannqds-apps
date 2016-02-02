package com.bormannqds.apps.wjh.lib.resources.tradingdata;

import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;

import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.CSV.AbstractTimeSeriesCSVParserStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.net.URL;
import java.util.Date;

class CSVParserFillStream extends AbstractTimeSeriesCSVParserStream<Fill> {

	public CSVParserFillStream(final URL baseLocator, final String strategy, final Date date, final CSVFormat csvFormat) throws ResourceNotFoundException {
		super(DataAccessUtils.createFillsLocator(baseLocator, strategy, date), csvFormat);
	}

	public BookingData getBookingData() {
		return bookingData;
	}

	// -------- Protected ----------

	@Override
	protected Fill createSample(final CSVRecord record) {
		if (bookingData == null) {
			bookingData = new BookingData(null, null, record.get(strategyFieldNdx));
		}

		return new Fill(parseTimestampField(record),
							bookingData,
							record.get(parentOrderIdFieldNdx),
							record.get(tickerFieldNdx),
							parseIntField(ORDERID_FIELDNAME, record.get(orderIdFieldNdx)),
							parseIntField(FILLID_FIELDNAME, record.get(fillIdFieldNdx)),
							parseIntField(FILLSIZE_FIELDNAME, record.get(fillSizeFieldNdx)),
							parseDoubleField(PRICE_FIELDNAME, record.get(priceFieldNdx)));
	}

	@Override
	protected void initialiseSpecificFieldIndices() {
		strategyFieldNdx = getHeaderMap().get(STRATEGY_FIELDNAME);
		parentOrderIdFieldNdx = getHeaderMap().get(PARENTID_FIELDNAME);
		tickerFieldNdx = getHeaderMap().get(TICKER_FIELDNAME);
		orderIdFieldNdx = getHeaderMap().get(ORDERID_FIELDNAME);
		fillIdFieldNdx = getHeaderMap().get(FILLID_FIELDNAME);
		fillSizeFieldNdx = getHeaderMap().get(FILLSIZE_FIELDNAME);
		priceFieldNdx = getHeaderMap().get(PRICE_FIELDNAME);
	}

	// -------- Private ----------

	private static final String STRATEGY_FIELDNAME = "strategy";
	private static final String PARENTID_FIELDNAME = "parentid";
	private static final String TICKER_FIELDNAME = "ticker";
	private static final String ORDERID_FIELDNAME = "orderid";
	private static final String FILLID_FIELDNAME = "fid";
	private static final String FILLSIZE_FIELDNAME = "fillsize";
	private static final String PRICE_FIELDNAME = "price";

	private BookingData bookingData = null;
	private int strategyFieldNdx;
	private int parentOrderIdFieldNdx;
	private int tickerFieldNdx;
	private int orderIdFieldNdx;
	private int fillIdFieldNdx;
	private int priceFieldNdx;
	private int fillSizeFieldNdx;
}

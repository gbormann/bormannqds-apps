package com.bormannqds.apps.wjh.lib.resources.ptadata;

import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.OrderState;

import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.CSV.AbstractTimeSeriesCSVParserStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.net.URL;

public class CSVParserPtaItemStream extends AbstractTimeSeriesCSVParserStream<PtaItem> {
	public CSVParserPtaItemStream(final URL baseLocator, final String parentOrderId, final CSVFormat csvFormat) throws ResourceNotFoundException {
		super(DataAccessUtils.createPtaLocator(baseLocator, parentOrderId), csvFormat);
	}

	@Override
	protected void initialiseSpecificFieldIndices() {
		tickerFieldNdx = getHeaderMap().get(TICKER_FIELDNAME);
		eventFieldNdx = getHeaderMap().get(EVENT_FIELDNAME);
		data1FieldNdx = getHeaderMap().get(DATA1_FIELDNAME);
		data2FieldNdx = getHeaderMap().get(DATA2_FIELDNAME);
		data3FieldNdx = getHeaderMap().get(DATA3_FIELDNAME);
		data4FieldNdx = getHeaderMap().get(DATA4_FIELDNAME);
		data5FieldNdx = getHeaderMap().get(DATA5_FIELDNAME);
	}

	@Override
	protected PtaItem createSample(CSVRecord record) {
		PtaItem ptaItem = null;
		char eventCode = record.get(eventFieldNdx).charAt(0); // assume it exists; otherwise crash&burn
		switch (eventCode) {
		case 'Q':
			ptaItem = new SummaryQuote(parseTimestampField(record),
					record.get(tickerFieldNdx),
					parseDoubleField(DATA1_FIELDNAME, record.get(data1FieldNdx)),
					parseDoubleField(DATA2_FIELDNAME, record.get(data2FieldNdx)),
					parseIntField(DATA3_FIELDNAME, record.get(data3FieldNdx)),
					parseIntField(DATA4_FIELDNAME, record.get(data4FieldNdx)));
			break;
		case 'T':
			ptaItem = new SummaryTrade(parseTimestampField(record),
					record.get(tickerFieldNdx),
					parseDoubleField(DATA1_FIELDNAME, record.get(data1FieldNdx)),
					parseIntField(DATA2_FIELDNAME, record.get(data2FieldNdx)));
			break;
		case 'O':
			ptaItem = new SummaryOrder(parseTimestampField(record),
					record.get(tickerFieldNdx),
					parseDoubleField(DATA1_FIELDNAME, record.get(data1FieldNdx)),
					parseIntField(DATA2_FIELDNAME, record.get(data2FieldNdx)),
					parseIntField(DATA3_FIELDNAME, record.get(data3FieldNdx)),
					OrderState.valueOf(record.get(data4FieldNdx)),
					parseLongField(DATA5_FIELDNAME, record.get(data5FieldNdx)));
			break;
		case 'F':
			ptaItem = new SummaryFill(parseTimestampField(record),
					record.get(tickerFieldNdx),
					parseDoubleField(DATA1_FIELDNAME, record.get(data1FieldNdx)),
					parseIntField(DATA2_FIELDNAME, record.get(data2FieldNdx)),
					parseIntField(DATA3_FIELDNAME, record.get(data3FieldNdx)),
					parseIntField(DATA4_FIELDNAME, record.get(data4FieldNdx)));
			break;
		default:
			break;
		}

		return ptaItem != null ? ptaItem : new NullItem(parseTimestampField(record));
	}

	private static final String TICKER_FIELDNAME = "ticker";
	private static final String EVENT_FIELDNAME = "event";
	private static final String DATA1_FIELDNAME = "data1";
	private static final String DATA2_FIELDNAME = "data2";
	private static final String DATA3_FIELDNAME = "data3";
	private static final String DATA4_FIELDNAME = "data4";
	private static final String DATA5_FIELDNAME = "data5";

	private int tickerFieldNdx;
	private int eventFieldNdx;
	private int data1FieldNdx;
	private int data2FieldNdx;
	private int data3FieldNdx;
	private int data4FieldNdx;
	private int data5FieldNdx;
}

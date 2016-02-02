package com.bormannqds.apps.wjh.lib.resources.tradingdata;

import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;

import com.bormannqds.lib.dataaccess.resources.ResourceNotFoundException;
import com.bormannqds.lib.dataaccess.timeseries.CSV.AbstractTimeSeriesCSVParserStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.net.URL;
import java.util.Date;

class CSVParserOrderStream extends AbstractTimeSeriesCSVParserStream<Order> {
	public CSVParserOrderStream(final URL baseLocator, final String strategy, final Date date, final CSVFormat csvFormat) throws ResourceNotFoundException {
		super(DataAccessUtils.createOrdersLocator(baseLocator, strategy, date), csvFormat);
	}

	public BookingData getBookingData() {
		return bookingData;
	}

	// -------- Protected ----------

	@Override
	protected Order createSample(final CSVRecord record) {
		if (bookingData == null) {
			bookingData = new BookingData(record.get(bookFieldNdx), record.get(traderFieldNdx), record.get(strategyFieldNdx));
		}

		return new Order(parseTimestampField(record),
							bookingData,
							AltId.valueOf(record.get(altIdFieldNdx)),
							record.get(parentOrderIdFieldNdx),
							LegTag.valueOf(record.get(tagFieldNdx)),
							record.get(tickerFieldNdx),
							findOrderState(record),
							parseIntField(ORDERID_FIELDNAME, record.get(orderIdFieldNdx)),
							parseIntField(SIZE_FIELDNAME, record.get(signedSizeFieldNdx)),
							parseDoubleField(LIMITPRICE_FIELDNAME, record.get(limitPriceFieldNdx)));
	}

	@Override
	protected void initialiseSpecificFieldIndices() {
		bookFieldNdx = getHeaderMap().get(BOOK_FIELDNAME);
		traderFieldNdx = getHeaderMap().get(TRADER_FIELDNAME);
		strategyFieldNdx = getHeaderMap().get(STRATEGY_FIELDNAME);
		altIdFieldNdx = getHeaderMap().get(ALTID_FIELDNAME);
		parentOrderIdFieldNdx = getHeaderMap().get(PARENTID_FIELDNAME);
		tagFieldNdx = getHeaderMap().get(TAG_FIELDNAME);
		tickerFieldNdx = getHeaderMap().get(TICKER_FIELDNAME);
		orderIdFieldNdx = getHeaderMap().get(ORDERID_FIELDNAME);
		limitPriceFieldNdx = getHeaderMap().get(LIMITPRICE_FIELDNAME);
		signedSizeFieldNdx = getHeaderMap().get(SIZE_FIELDNAME);

		if (stateFieldNdcs.length != orderStates.length) {
			throw new IndexOutOfBoundsException("BUG: #state fields does not match #order states!");
		}

		stateFieldNdcs[OrderState.PENDING_NEW.ordinal()] = getHeaderMap().get(REQUESTED_FIELDNAME);
		stateFieldNdcs[OrderState.NEW_ORDER_SINGLE.ordinal()] = getHeaderMap().get(SUBMITTED_FIELDNAME);
		stateFieldNdcs[OrderState.PENDING_AMEND.ordinal()] = getHeaderMap().get(AMENDREQUESTED_FIELDNAME);
		stateFieldNdcs[OrderState.AMENDED.ordinal()] = getHeaderMap().get(AMENDCONFIRMED_FIELDNAME);
		stateFieldNdcs[OrderState.PENDING_CANCEL.ordinal()] = getHeaderMap().get(CANCELREQUEST_FIELDNAME);
		stateFieldNdcs[OrderState.CANCELED.ordinal()] = getHeaderMap().get(CANCELLED_FIELDNAME);
		stateFieldNdcs[OrderState.FILLED.ordinal()] = getHeaderMap().get(FILLED_FIELDNAME);
		stateFieldNdcs[OrderState.REJECTED.ordinal()] = getHeaderMap().get(REJECTED_FIELDNAME);
	}

	// -------- Private ----------

	private OrderState findOrderState(final CSVRecord record) {
		// linear search will do
		for (int stateNdx = 0; stateNdx < stateFieldNdcs.length; ++stateNdx) {
			if (record.get(stateFieldNdcs[stateNdx]).charAt(0) == 't') {
				return orderStates[stateNdx];
			}
		}
		return null;
	}

	private static final String BOOK_FIELDNAME = "book";
	private static final String TRADER_FIELDNAME = "trader";
	private static final String STRATEGY_FIELDNAME = "strategy";
	private static final String ALTID_FIELDNAME = "altid";
	private static final String PARENTID_FIELDNAME = "parentid";
	private static final String TAG_FIELDNAME = "tag";
	private static final String TICKER_FIELDNAME = "ticker";
	private static final String ORDERID_FIELDNAME = "orderid";
	private static final String LIMITPRICE_FIELDNAME = "limitprice";
	private static final String SIZE_FIELDNAME = "signedsize";

	private static final String REQUESTED_FIELDNAME = "requested";
	private static final String SUBMITTED_FIELDNAME = "submitted";
	private static final String AMENDREQUESTED_FIELDNAME = "amendrequested";
	private static final String AMENDCONFIRMED_FIELDNAME = "amendconfirmed";
	private static final String CANCELREQUEST_FIELDNAME = "cancelrequest";
	private static final String CANCELLED_FIELDNAME = "cancelled";
	private static final String FILLED_FIELDNAME = "filled";
	private static final String REJECTED_FIELDNAME = "rejected";

	private static final OrderState[] orderStates =
		{
			OrderState.PENDING_NEW,
			OrderState.NEW_ORDER_SINGLE,
			OrderState.PENDING_AMEND,
			OrderState.AMENDED,
			OrderState.PENDING_CANCEL,
			OrderState.CANCELED,
			OrderState.FILLED,
			OrderState.REJECTED
		};

	private BookingData bookingData = null;
	private int bookFieldNdx;
	private int traderFieldNdx;
	private int strategyFieldNdx;
	private int altIdFieldNdx;
	private int parentOrderIdFieldNdx;
	private int tagFieldNdx;
	private int tickerFieldNdx;
	private int orderIdFieldNdx;
	private int limitPriceFieldNdx;
	private int signedSizeFieldNdx;

	private final int[] stateFieldNdcs = new int[8];
}

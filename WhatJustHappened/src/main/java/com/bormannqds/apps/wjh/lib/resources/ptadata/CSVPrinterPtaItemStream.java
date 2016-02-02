package com.bormannqds.apps.wjh.lib.resources.ptadata;

import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.timeseries.CSV.AbstractTimeSeriesCSVPrinterStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;

public class CSVPrinterPtaItemStream extends AbstractTimeSeriesCSVPrinterStream<PtaItem> {

	public CSVPrinterPtaItemStream(final URL sinkLocator, final CSVFormat csvFormat) {
		super(sinkLocator, csvFormat.withHeader(PTA_HEADER.split(",")));
	}

	// -------- Protected ----------

	@Override
	protected void putRecord(final CSVPrinter printer, final PtaItem ptaItem) throws ResourceIOException {
		try {
			switch(ptaItem.getEvent()) {
			case 'Q':
				SummaryQuote quote = (SummaryQuote)ptaItem;
				printer.printRecord(formatTimestampField(quote.getTimestamp()),
									quote.getTicker(),
									quote.getEvent(),
									quote.getAsk(), quote.getBid(),
									quote.getAskSize(), quote.getBidSize());
				break;
			case 'T':
				SummaryTrade trade = (SummaryTrade)ptaItem;
				printer.printRecord(formatTimestampField(trade.getTimestamp()),
						trade.getTicker(),
						trade.getEvent(),
						trade.getPrice(), trade.getSize());
				break;
			case 'O':
				SummaryOrder order = (SummaryOrder)ptaItem;
				printer.printRecord(formatTimestampField(order.getTimestamp()),
						order.getTicker(),
						order.getEvent(),
						order.getPrice(), order.getSize(),
						order.getOrderId(), order.getOrderState().name(),
						order.getLifeTime());
				break;
			case 'F':
				SummaryFill fill = (SummaryFill)ptaItem;
				printer.printRecord(formatTimestampField(fill.getTimestamp()),
						fill.getTicker(),
						fill.getEvent(),
						fill.getPrice(), fill.getSize(),
						fill.getOrderId(), fill.getFillId());
				break;
			default: // BUG: should never happen!
				return;	
			}
		}
		catch (IOException ioe) {
			LOGGER.error("Exception caught whilst trying to a print record to a CSV stream resource at " + getLocator(), ioe);
			throw new ResourceIOException("Exception caught whilst trying to a print record to a CSV stream resource at " + getLocator(), ioe);
		}
	}

	// -------- Private ----------

	private static final Logger LOGGER = LogManager.getLogger(CSVPrinterPtaItemStream.class);
	private static final String PTA_HEADER = "ts,ticker,event,data1,data2,data3,data4,data5";
}

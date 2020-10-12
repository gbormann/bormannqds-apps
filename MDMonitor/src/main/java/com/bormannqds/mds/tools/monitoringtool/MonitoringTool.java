package com.bormannqds.mds.tools.monitoringtool;

import com.bormannqds.mds.lib.protobufmessages.MarketData;
import com.bormannqds.mds.lib.protocoladaptor.utils.ChronoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * Created by bormanng on 10/07/15.
 */
public class MonitoringTool {
    public void initialise(final Map<String, List<String>> interestListMap) {
        for (Map.Entry<String, List<String>> entry: interestListMap.entrySet()) {
            mdModel.registerGroup(entry.getKey(), entry.getValue().size());
        }
    }

    public TableModel getMarketDataModel() {
        return mdModel;
    }

    public void scheduleMarketDataModelUpdate(final String group, final MarketData.MarketDataMessage marketData) {
        SwingUtilities.invokeLater(new MarketDataModelUpdater(group, marketData));
    }

    private static class MarketDataModel extends DefaultTableModel {
        public MarketDataModel() {
            super(null, COLUMN_HEADERS);
        }

        @Override
        public Class<?> getColumnClass(int colNdx) {
            return COLUMN_TYPES[colNdx];
        }

        @Override
        public boolean isCellEditable(int rowNdx, int colNdx) {
            return false;
        }

        public void registerGroup(final String group, int groupSize) {
            int base = getRowCount();
            groupRowBaseIndexMap.put(group, base);
            Object[] firstGroupRow = new Object[COLUMN_HEADERS.length];
            firstGroupRow[Columns.GROUP.ordinal()] = group;
            addRow(firstGroupRow);
            for (int cnt = 1; cnt < groupSize; ++cnt) {
                addRow(new Object[COLUMN_HEADERS.length]);
            }
        }

        public void updateRow(final String group, final MarketData.MarketDataMessage marketData) {
            final String symbol = marketData.getSymbol();
            boolean firstForSymbol = false;
            Integer rowOffset = symbolRowOffsetMap.get(symbol);
            if (rowOffset == null) {
                firstForSymbol = true;
                rowOffset = groupMaxRowOffsetMap.merge(group, 0, (v, v0)->v+1);
                symbolRowOffsetMap.put(symbol, rowOffset);
            }
            int rowNdx = groupRowBaseIndexMap.get(group) + rowOffset;
            Vector<Object> rowData = (Vector<Object>) getDataVector().get(rowNdx);
            if (firstForSymbol) {
                rowData.setElementAt(symbol, Columns.SYMBOL.ordinal());
            }
            rowData.setElementAt(ChronoUtils.micros2TimestampMs(marketData.getTimestamp()),
                                    Columns.LAST_UPDATE.ordinal());
            switch(marketData.getPayloadCase()) {
                case TOB_QUOTE:
                    MarketData.TobQuote tobQuote = marketData.getTobQuote();
                    rowData.setElementAt(tobQuote.getAskSize(), Columns.ASK_SIZE.ordinal());
                    rowData.setElementAt(tobQuote.getAsk(), Columns.ASK.ordinal());
                    rowData.setElementAt(tobQuote.getBid(), Columns.BID.ordinal());
                    rowData.setElementAt(tobQuote.getBidSize(), Columns.BID_SIZE.ordinal());
                    break;
                case TRADE:
                    MarketData.Trade trade = marketData.getTrade();
                    rowData.setElementAt(trade.getPrice(), Columns.LAST_PRICE.ordinal());
                    rowData.setElementAt(trade.getSize(), Columns.LAST_SIZE.ordinal());
                    break;
                case PAYLOAD_NOT_SET:
                default:
                    StringBuilder commentBuilder = new StringBuilder(symbolDroppedMsgMap.merge(symbol, 1, (v, v0) -> v + 1));
                    commentBuilder.append(" bad msgs");
                    rowData.setElementAt(commentBuilder.toString(), rowNdx);
                    LOGGER.warn(symbol + ": msg dropped");
                    return;
            }
            fireTableRowsUpdated(rowNdx, rowNdx);
        }

        private enum Columns {
            GROUP("group", String.class), SYMBOL("ticker", String.class), S("S", Character.class),
            LAST_UPDATE("update time(ms)", String.class),
            ASK_SIZE("ask size", Integer.class), ASK("ask", Double.class), BID("bid", Double.class), BID_SIZE("bid size", Integer.class),
            LAST_PRICE("last price", Double.class), LAST_SIZE("last size", Integer.class),
            COMMENT("comment", String.class),
            EOF("<delimiter>", Object.class);

            public String getHeader() {
                return header;
            }

            public Class<?> getType() {
                return type;
            }

            Columns(final String header, final Class<?> type) {
                this.header = header;
                this.type = type;
            }

            private final String header;
            private final Class<?> type;
        }

        private static final String[] COLUMN_HEADERS = new String[Columns.EOF.ordinal()];
        private static final Class<?>[] COLUMN_TYPES = new Class[Columns.EOF.ordinal()];
        static {
            for (Columns column: Columns.values()) {
                if (column != Columns.EOF) {
                    COLUMN_HEADERS[column.ordinal()] = column.getHeader();
                    COLUMN_TYPES[column.ordinal()] = column.getType();
                }
            }
        }

        private final Map<String, Integer> groupRowBaseIndexMap = new HashMap<>();
        private final Map<String, Integer> groupMaxRowOffsetMap = new HashMap<>();
        private final Map<String, Integer> symbolRowOffsetMap = new HashMap<>();
        private final Map<String, Integer> symbolDroppedMsgMap = new HashMap<>(); // start with simple count
    }

    private class MarketDataModelUpdater implements Runnable {
        public MarketDataModelUpdater(final String group, final MarketData.MarketDataMessage marketDataMessage) {
            this.group = group;
            this.marketDataMessage = marketDataMessage;
        }

        @Override
        public void run() {
            mdModel.updateRow(group, marketDataMessage);
        }

        private final String group;
        private final MarketData.MarketDataMessage marketDataMessage;
    }

    private static final Logger LOGGER = LogManager.getLogger(MonitoringTool.class);

    private final MarketDataModel mdModel = new MarketDataModel();
}

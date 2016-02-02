package com.bormannqds.apps.wjh.lib.referencedata;

import com.bormannqds.lib.dataaccess.referencedata.*;
import com.bormannqds.lib.dataaccess.resources.ResourceNotOpenException;
import com.bormannqds.lib.dataaccess.resources.XmlResource;
import nu.xom.Node;
import nu.xom.Nodes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ReferenceDataResource extends XmlResource implements ReferenceDataResourceInterface
{
//public:
	public ReferenceDataResource() {
		super(RSC_NAME);
	}

	public ReferenceDataResource(final URL locator) {
		super(RSC_NAME, locator);
	}

    /**
     * Get generic instrument via internal base symbol.
     * @param baseSymbol - base symbol from internal symbology
     * @return - generic Instrument
     * @throws MissingInstrumentException
     * @throws UnsupportedInstrumentTypeException
     */
	public Instrument getInstrument(final String baseSymbol) throws MissingInstrumentException, UnsupportedInstrumentTypeException {
        if (!isOpen()) {
            LOGGER.error("BUG: Use of resource before it's opened!");
            throw new ResourceNotOpenException();
        }

        Instrument instrument = instruments.get(baseSymbol);
        if (instrument == null) { // not even in the instruments cache (by base symbol) so try resource
            final Node productNode = getProductNodes(baseSymbol, GENERIC_QUERY_BUILDER).get(0); // take the first one

            // If we crash with an NPE or format error in the last stretch, it's because the XML is not valid. If only we had a DTD!
            InstrumentType instrumentType = InstrumentType.valueOf(productNode.query("productType").get(0).getValue());
            switch(instrumentType) {
                case Future: instrument = createAndRegisterFuture(productNode); break;
                case Spread: instrument = createAndRegisterSpread(productNode); break;
                default: throw new UnsupportedInstrumentTypeException(instrumentType + ": unsupported instrument type!");
            }
        }

        return instrument;
    }

	/**
	 * Get future instrument by internal symbol.
	 * 
	 * @param symbol - symbol from internal symbology
	 * @return - Future implementation of Instrument
	 *
	 */
	public Future getFuture(final String symbol) throws ResourceNotOpenException, UnrecognisedSymbolException, NotAFutureSymbolException, MissingInstrumentException {
		if (!isOpen()) {
			LOGGER.error("BUG: Use of resource before it's opened!");
			throw new ResourceNotOpenException();
		}

		Future future = null;
        Instrument instrument = null;
        final InternalSymbolParser symbolParser = InternalSymbolParser.getInstance();
        if (symbolParser.isFqSymbol(symbol)) {
            instrument = contracts.get(symbol);
        }
		if (instrument == null) { // not in the contracts cache (by symbol)
            final List<ExpiringInstrumentSymbolComponents> symbolComponents = symbolParser.parse(symbol);
            if (symbolComponents.size() > 1) throw new NotAFutureSymbolException(symbol + " doesn't appear to be a future symbol");
            final String baseSymbol = symbolComponents.get(0).getBaseSymbol(); // just short-hand
			instrument = instruments.get(baseSymbol);
			if (instrument == null) { // not even in the instruments cache (by naked base symbol) so try resource
				future = createAndRegisterFuture(getProductNodes(baseSymbol, FUTURE_QUERY_BUILDER).get(0));
			}
			LOGGER.debug("Adding " + symbol + " to contracts cache...");
			contracts.put(symbol, instrument);
		}

		if (future != null)
			return future; // save a cast to Future if we just have built a new one...

		return (Future)instrument;
	}

	/**
	 * Get spread instrument by internal symbol. Currently, synthetic spreads can only be retrieved by base symbols
     * because FQ symbols are hard to invert to the generic base symbols that could generate them.
	 *
	 * @param symbol - symbol from internal symbology
	 * @return - Spread implementation of Instrument
     * @throws ResourceNotOpenException
     * @throws UnrecognisedSymbolException
     * @throws MissingInstrumentException
     */
	public Spread getSpread(final String symbol) throws ResourceNotOpenException, UnrecognisedSymbolException, NotAnXchSpreadSymbolException, MissingInstrumentException {
		if (!isOpen()) {
			LOGGER.error("BUG: Use of resource before it's opened!");
			throw new ResourceNotOpenException();
		}

		Spread spread = null;
        Instrument instrument = null;
        final InternalSymbolParser symbolParser = InternalSymbolParser.getInstance();
        if (symbolParser.isFqSymbol(symbol)) {
            instrument = contracts.get(symbol);
        }
		if (instrument == null) { // not in the contracts cache (by symbol)
            final List<ExpiringInstrumentSymbolComponents> symbolComponents = symbolParser.parse(symbol);
            if (symbolComponents.size() > 1) throw new NotAnXchSpreadSymbolException(symbol + "doesn't appear to be an exchange spread");
            final String baseSymbol = symbolComponents.get(0).getBaseSymbol(); // just short-hand
			instrument = instruments.get(baseSymbol);
			if (instrument == null) { // not even in the instruments cache (by naked base symbol) so try resource
                spread = createAndRegisterSpread(getProductNodes(baseSymbol, SPREAD_QUERY_BUILDER).get(0));
			}
			LOGGER.debug("Adding " + symbol + " to contracts cache...");
			contracts.put(symbol, instrument);
		}

		if (spread != null)
			return spread; // save a cast to Spread if we just have built a new one...

		return (Spread)instrument;
	}

//private:
    private static class XPathQueryBuilder {
        public XPathQueryBuilder() {
            querySuffix = "']";
        }

        public XPathQueryBuilder(final InstrumentType instrumentType) {
            StringBuilder suffixBuilder = new StringBuilder("' and productType=")
                    .append(instrumentType.getXPathTestValue())
                    .append(']');
            querySuffix = suffixBuilder.toString();
        }

        public String build(final String basesymbol) {
            StringBuilder xpathBuilder = new StringBuilder(QUERY_PREFIX).append(basesymbol).append(querySuffix);
            return xpathBuilder.toString();
        }

        private static final String QUERY_PREFIX = "/symbols/symbol[local='";
        private final String querySuffix;
    }

    private Nodes getProductNodes(final String basesymbol, final XPathQueryBuilder queryBuilder) throws MissingInstrumentException {
        Nodes productNodes = document.query(queryBuilder.build(basesymbol));
        if (productNodes.size() == 0) {
            LOGGER.warn(basesymbol + ": no reference data found!");
            throw new MissingInstrumentException(basesymbol + ": no reference data found!");
        }
        if (productNodes.size() > 1) { // We should only get one! If not, the ref data XML is fishy.
            LOGGER.warn(basesymbol + ": multiple entries found!");
        }
        return productNodes;
    }

    private Future createAndRegisterFuture(final Node productNode) {
        // If we crash with an NPE or format error in the last stretch, it's because the XML is not valid. If only we had a DTD!
        final Currency ccy = Currency.valueOf(productNode.query("currency").get(0).getValue());
        double tickSize = Double.parseDouble(productNode.query("tickSize").get(0).getValue());
        double tickValue = Double.parseDouble(productNode.query("tickValue").get(0).getValue());
        Future future = new Future(new HashSet<String>(), ccy, tickSize, tickValue);
        registerProduct(future, productNode);
        return future;
    }

    private Spread createAndRegisterSpread(final Node productNode) {
        // If we crash with an NPE or format error in the last stretch, it's because the XML is not valid. If only we had a DTD!
        double tickSize = Double.parseDouble(productNode.query("tickSize").get(0).getValue());
        double tickValue = Double.parseDouble(productNode.query("tickValue").get(0).getValue());
        final Currency ccy = Currency.valueOf(productNode.query("currency").get(0).getValue());
        Spread spread = new Spread(new HashSet<String>(), ccy, tickSize, tickValue);
        registerProduct(spread, productNode);
        return spread;
    }

	private void registerProduct(final Instrument instrument, final Node productNode) {
		final Nodes baseSymbolNodes = productNode.query("local"); // includes the derived internal base symbol
		for (int i = 0; i < baseSymbolNodes.size(); ++i) {
			final Node baseSymbolNode = baseSymbolNodes.get(i);
			instrument.getInternalBaseSymbols().add(baseSymbolNode.getValue());
			LOGGER.debug("Adding " + baseSymbolNode.getValue() + " to instruments cache...");
			instruments.put(baseSymbolNode.getValue(), instrument);
		}
	}

	private static final Logger LOGGER = LogManager.getLogger(ReferenceDataResource.class);
    private static final String RSC_NAME = "Reference Data";
    private static final XPathQueryBuilder GENERIC_QUERY_BUILDER = new XPathQueryBuilder();
    private static final XPathQueryBuilder FUTURE_QUERY_BUILDER = new XPathQueryBuilder(InstrumentType.Future);
    private static final XPathQueryBuilder SPREAD_QUERY_BUILDER = new XPathQueryBuilder(InstrumentType.Spread);

	private final Map<String, Instrument> instruments = new HashMap<>(); // by internal base symbol in method call occurrence
	private final Map<String, Instrument> contracts = new HashMap<>(); // by internal symbol
}

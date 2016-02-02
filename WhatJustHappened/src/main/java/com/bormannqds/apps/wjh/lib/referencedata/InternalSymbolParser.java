package com.bormannqds.apps.wjh.lib.referencedata;

import com.bormannqds.lib.dataaccess.referencedata.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Agora internal symbols for expiring instruments, i.e. Futures, exchange Future Spreads
 * and synthetic Future Spreads.
 *
 * Regexp for cross-product (i.e. outright/exchange spread or synthetic spread) generic internal symbol parsing:
 * (([0-9]*[A-Z]+)(?>#([1-9][0-9]*)|([0-9]{2}(?>0[1-9]|1[012])))?)(?>-((?>[0-9]*[A-Z]+(?>#[1-9][0-9]*|[0-9]{2}(?>0[1-9]|1[012]))?-)*(?>[0-9]*[A-Z]+(?>#[1-9][0-9]*|[0-9]{2}(?>0[1-9]|1[012]))?){1}))?
 * Groups:
 *  0: ALL
 *  1: full symbol (of first leg if basket symbol)
 *  2: base ticker of outright, exchange spread or first leg of synthetic spread
 *  /3: n-th front month or null of outright, exchange spread or first leg
 *  \4: null or maturity code of outright, exchange spread or first leg
 *  5: null (=> not a synthetic spread) or remainder symbol (first leg stripped)
 *
 * The first subexpr (([0-9]*[A-Z]+)(?>#([1-9][0-9]*)|([0-9]{2}(?>0[1-9]|1[012])))?) matches any leg or outright
 *      a. naked base ticker
 *      b. generic n-th front month base ticker
 *      c. FQ internal symbol with maturity code
 *
 * Created by guy on 20/07/15.
 */
public class InternalSymbolParser implements ExpiringInstrumentSymbolParser {
    public static InternalSymbolParser getInstance() {
        return instance;
    }

    public boolean isFqSymbol(final String symbol) {
        boolean isFqSymbol = false;
        Matcher matcher = symbolPattern.matcher(symbol);
        if (matcher.matches()) {
            isFqSymbol = matcher.group(RegexpGroup.EXP_CODE.ordinal()) != null;
            for (;
                 isFqSymbol && matcher.group(RegexpGroup.REM_SYM.ordinal()) != null;
                 matcher = symbolPattern.matcher(matcher.group(RegexpGroup.REM_SYM.ordinal()))) {
                isFqSymbol = matcher.group(RegexpGroup.EXP_CODE.ordinal()) != null;
            }
        }

        return isFqSymbol;
    }

    public List<ExpiringInstrumentSymbolComponents> parseExceptFqSymbols(final String symbol) throws UnrecognisedSymbolException {
        return parse(symbol, true);
    }

    public List<ExpiringInstrumentSymbolComponents> parse(final String symbol) throws UnrecognisedSymbolException {
        return parse(symbol, false);
    }

	// -------- Private ----------

    private enum RegexpGroup {
        ALL, LEG_SYM, BASE_SYM, FM_RANK, EXP_CODE, REM_SYM
    }

    private InternalSymbolParser() {

    }

    private ExpiringInstrumentSymbolComponents parseLeg(Matcher matcher, boolean idMapFqs) {
        if (matcher.group(RegexpGroup.EXP_CODE.ordinal()) == null) { // generic symbol
            if (matcher.group(RegexpGroup.FM_RANK.ordinal()) == null) { // naked base symbol
                return new GenericSymbolComponents(matcher.group(RegexpGroup.BASE_SYM.ordinal()), null, (short)1);
            }
            else {
                short rank = Short.parseShort(matcher.group(RegexpGroup.FM_RANK.ordinal()));
                return new GenericSymbolComponents(matcher.group(RegexpGroup.BASE_SYM.ordinal()), null, rank);
            }
        }
        else {
            if (idMapFqs) {
                return new IdentitySymbolComponents(matcher.group(RegexpGroup.LEG_SYM.ordinal()), null);
            }
            else {
                final String expirySuffix = matcher.group(RegexpGroup.EXP_CODE.ordinal());
                short year = Short.parseShort(expirySuffix.substring(0, YEAR_LENGTH));
                short month = Short.parseShort(expirySuffix.substring(YEAR_LENGTH));
                return new FqSymbolComponents(matcher.group(RegexpGroup.BASE_SYM.ordinal()), null, year, month);
            }
        }
    }

    private List<ExpiringInstrumentSymbolComponents> parse(final String symbol, boolean idMapFqs) throws UnrecognisedSymbolException {
        Matcher matcher = symbolPattern.matcher(symbol);
        if (!matcher.matches()) {
            throw new UnrecognisedSymbolException(symbol + " doesn't appear to be a valid internal symbol!");
        }
        final List<ExpiringInstrumentSymbolComponents> legs = new ArrayList<>();
        legs.add(parseLeg(matcher, idMapFqs));
        for (;
             matcher.group(RegexpGroup.REM_SYM.ordinal()) != null;
             matcher = symbolPattern.matcher(matcher.group(RegexpGroup.REM_SYM.ordinal()))) {
            legs.add(parseLeg(matcher, idMapFqs));
        }
        return legs;
    }

    private static final String SYMBOL_REGEXP = "(([0-9]*[A-Z]+)(?>#([1-9][0-9]*)|([0-9]{2}(?>0[1-9]|1[012])))?)" /* outright, exchange spread or first leg of synthetic spread */
            + "(?>-((?>[0-9]*[A-Z]+(?>#[1-9][0-9]*|[0-9]{2}(?>0[1-9]|1[012]))?-)*" /* intermediate legs of synthetic if any */
            + "(?>[0-9]*[A-Z]+(?>#[1-9][0-9]*|[0-9]{2}(?>0[1-9]|1[012]))?){1}))?" /* last leg of synthetic if any */ ;
    private static final Pattern symbolPattern = Pattern.compile(SYMBOL_REGEXP);
    private static final int YEAR_LENGTH = 2;
    private static final InternalSymbolParser instance = new InternalSymbolParser();
}

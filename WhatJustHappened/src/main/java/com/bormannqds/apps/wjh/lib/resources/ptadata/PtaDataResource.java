package com.bormannqds.apps.wjh.lib.resources.ptadata;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.DataAccessUtils;

import com.bormannqds.lib.dataaccess.resources.*;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesInputResource;
import org.apache.commons.csv.CSVFormat;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *  Provides sequential access to PTA data (PTAD) files at the locatorBase (PTAD dir+strategy dir+date dir+parent order id).
 *  
 *  The individual collection members are identified by a parent order id key and the corresponding file names are derived
 *  as follows:
 *  	path name = PTAD directory+strategy dir+date (YYYY-MM-DD) dir+parent order id+suffix
 *  where
 *  	suffix = "_PTA.csv"
 */
public class PtaDataResource extends XmlResource {

	public PtaDataResource(final URL baseLocator, final String dateString, final String strategy) throws ResourceNotFoundException {
		super("PTA Data", DataAccessUtils.appendPtaBaseLocator(baseLocator, dateString, strategy));
		this.csvFormat = CSVFormat.DEFAULT.withRecordSeparator(ApplicationContext.getInstance().getConfigurationResource().getDefaultCsvRecordSeparator())
											.withHeader();
	}

	public TimeSeriesInputResource<PtaItem> getPtaItemStream(final String parentOrderId) throws ResourceNotFoundException {
		TimeSeriesInputResource<PtaItem> ptaItemStream = ptaItemStreamCache.get(parentOrderId);
		if (ptaItemStream == null) {
			ptaItemStream = new CSVParserPtaItemStream(getLocator(), parentOrderId, csvFormat);
			ptaItemStreamCache.put(parentOrderId, ptaItemStream);
		}
		return ptaItemStream;
	}

	public void dispose() throws ResourceIOException {
		for (TimeSeriesInputResource<PtaItem> ptaItemStream: ptaItemStreamCache.values()) {
			ptaItemStream.close();
		}
		ptaItemStreamCache.clear(); // clear the cache
	}

	// -------- Protected ---------

	@Override
	protected void reset() throws ResourceIOException {
		dispose();
	}

	// -------- Private ----------

	private final CSVFormat csvFormat;
	private final Map<String, TimeSeriesInputResource<PtaItem>> ptaItemStreamCache = new HashMap<String, TimeSeriesInputResource<PtaItem>>();
}

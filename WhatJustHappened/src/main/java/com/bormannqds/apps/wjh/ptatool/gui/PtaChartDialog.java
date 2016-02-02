package com.bormannqds.apps.wjh.ptatool.gui;

import com.bormannqds.apps.wjh.lib.gateway.ApplicationContext;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.LegTag;
import com.bormannqds.apps.wjh.lib.resources.tradingdata.OrderSide;
import com.bormannqds.apps.wjh.lib.timeseries.TimeSeriesKeys;
import com.bormannqds.apps.wjh.ptatool.PtaModel;
import com.bormannqds.apps.wjh.ptatool.TimeSeriesCollectionKeys;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.*;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public class PtaChartDialog extends JDialog {
	/**
	 * Create the dialog.
	 */
	public PtaChartDialog() {
		initialise();
	}

	public PtaModel getPtaModel() {
		return ptaModel;
	}

	public void prepareChartPanel(final String parentOrderId, final Map<LegTag, String> basketLegMap) {
		chartPanel.removeAll();
		chartPanel.setChart(createChart(parentOrderId, basketLegMap));
		pack();
	}

	// -------- Private ----------

	private class CloseAction extends AbstractAction {
		public CloseAction() {
			LOGGER.debug("Creating CloseAction for the close button...");
			putValue(ACTION_COMMAND_KEY, "HidePtaChartsDialog");
			putValue(NAME, "Close");
			putValue(SHORT_DESCRIPTION, "Hide the Post-trade Analysis Charts dialog");
		}

		public void actionPerformed(ActionEvent e) {
			LOGGER.debug("CloseAction fired!");
			setVisible(false);
			//setFocusable(false);
			ApplicationContext.getInstance().getAppStatusBean().setStatus("PTA tool: charts dialog closed.");
		}
	}

	private void setFillsRendererAttributes(final StandardXYItemRenderer renderer, boolean showInLegend) {
		renderer.setBaseShapesFilled(false);
		renderer.setAutoPopulateSeriesPaint(false);
		renderer.setAutoPopulateSeriesShape(false);
		renderer.setBasePaint(Color.WHITE);
		renderer.setSeriesPaint(TimeSeriesKeys.Fills.getNdx(), Color.CYAN);
		renderer.setSeriesShape(TimeSeriesKeys.Fills.getNdx(), ShapeUtilities.createDiamond(4.0F));
		renderer.setSeriesShapesFilled(TimeSeriesKeys.Fills.getNdx(), true);
		renderer.setSeriesVisibleInLegend(TimeSeriesKeys.Fills.getNdx(), showInLegend);
	}

	private void setTradesRendererAttributes(final StandardXYItemRenderer renderer, boolean showInLegend) {
		renderer.setBaseShapesFilled(false);
		renderer.setAutoPopulateSeriesPaint(false);
		renderer.setAutoPopulateSeriesShape(false);
		renderer.setBasePaint(Color.WHITE);
		renderer.setSeriesPaint(TimeSeriesKeys.Trades.getNdx(), Color.BLUE);
		Shape upTriangle = ShapeUtilities.createUpTriangle(4.0F);
		renderer.setSeriesShape(TimeSeriesKeys.Trades.getNdx(), ShapeUtilities.rotateShape(upTriangle, Math.PI / 2, 0.0F, 0.0F));
		renderer.setSeriesShapesFilled(TimeSeriesKeys.Trades.getNdx(), true);
		renderer.setSeriesVisibleInLegend(TimeSeriesKeys.Trades.getNdx(), showInLegend);
	}

	private void setOrdersRendererAttributes(final XYStepRenderer renderer, boolean showInLegend) {
		final float[] dashes = new float[] { 2.0F, 2.0F, 1.0F, 2.0F };

		renderer.setBaseShapesFilled(false);
		renderer.setAutoPopulateSeriesPaint(false);
		renderer.setAutoPopulateSeriesShape(false);
		renderer.setAutoPopulateSeriesStroke(false);
		renderer.setBasePaint(Color.WHITE);
		renderer.setSeriesPaint(TimeSeriesKeys.Orders.getNdx(), Color.MAGENTA);
		renderer.setSeriesStroke(TimeSeriesKeys.Orders.getNdx(), new BasicStroke(0.2F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0F, dashes, 0.0F));
		renderer.setSeriesVisibleInLegend(TimeSeriesKeys.Orders.getNdx(), showInLegend);
	}

	private void setEventsRendererAttributes(final StandardXYItemRenderer renderer, boolean showInLegend) {
		renderer.setBaseShapesFilled(false);
		renderer.setAutoPopulateSeriesPaint(false);
		renderer.setAutoPopulateSeriesShape(false);
		renderer.setBasePaint(Color.WHITE);
		renderer.setSeriesPaint(TimeSeriesKeys.Events.getNdx(), Color.MAGENTA);
		renderer.setSeriesShape(TimeSeriesKeys.Events.getNdx(), new Rectangle(-2, -3, 4, 6));
		renderer.setSeriesShapesFilled(TimeSeriesKeys.Events.getNdx(), true);
		renderer.setSeriesVisibleInLegend(TimeSeriesKeys.Events.getNdx(), showInLegend);
	}

	private void setQuotesRendererAttributes(final XYStepRenderer renderer, boolean showInLegend) {
		renderer.setBaseShapesFilled(false);
		renderer.setAutoPopulateSeriesPaint(false);
		renderer.setAutoPopulateSeriesShape(false);
		renderer.setAutoPopulateSeriesStroke(false);
		renderer.setBasePaint(Color.WHITE);
		renderer.setSeriesPaint(TimeSeriesKeys.Bids.getNdx(), Color.GREEN);
		renderer.setSeriesPaint(TimeSeriesKeys.Asks.getNdx(), Color.RED);
		renderer.setSeriesStroke(TimeSeriesKeys.Bids.getNdx(), new BasicStroke(1.0F));
		renderer.setSeriesStroke(TimeSeriesKeys.Asks.getNdx(), new BasicStroke(1.0F));
		renderer.setSeriesVisibleInLegend(TimeSeriesKeys.Bids.getNdx(), showInLegend);
		renderer.setSeriesVisibleInLegend(TimeSeriesKeys.Asks.getNdx(), showInLegend);
	}

	private void setModelVarsRendererAttributes(final XYStepRenderer renderer, TimeSeriesKeys modelVarToPlot, boolean showInLegend) {
		renderer.setBaseShapesFilled(false);
		renderer.setAutoPopulateSeriesPaint(false);
		renderer.setAutoPopulateSeriesShape(false);
		renderer.setAutoPopulateSeriesStroke(false);
		renderer.setBasePaint(Color.WHITE);
		renderer.setSeriesPaint(modelVarToPlot.getNdx(), Color.YELLOW);
		renderer.setSeriesStroke(modelVarToPlot.getNdx(), new BasicStroke(0.4F));
		renderer.setSeriesVisibleInLegend(modelVarToPlot.getNdx(), showInLegend);
	}

	private JFreeChart createChart(final String parentOrderId, final Map<LegTag, String> basketLegMap) {
    	Map<String, Map<TimeSeriesCollectionKeys, TimeSeriesCollection>> chartModels = ptaModel.getChartModels();
    	Map<String, OrderSide> legSideMap = ptaModel.getLegSides();
    	List<XYPlot> legPlots = new ArrayList<XYPlot>();
    	for (Entry<LegTag, String> basketLeg : basketLegMap.entrySet()) {
    		// Primary dataset (i.e. Fills, which will always be on top) and its renderer:
            Map<TimeSeriesCollectionKeys, TimeSeriesCollection> legCollections = chartModels.get(basketLeg.getValue());
        	XYDataset primaryDataset = legCollections.get(TimeSeriesCollectionKeys.Fills);
    		StringBuilder rangeAxisNameBuilder = new StringBuilder("Price ");
    		rangeAxisNameBuilder.append(basketLeg.getKey().toString()).append(':').append(legSideMap.get(basketLeg.getValue())).append(' ').append(basketLeg.getValue());
            NumberAxis priceAxis = new NumberAxis(rangeAxisNameBuilder.toString()); 
            priceAxis.setAutoRange(true);
            priceAxis.setAutoRangeIncludesZero(false);
            StandardXYItemRenderer fillsRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
            setFillsRendererAttributes(fillsRenderer, basketLeg.getKey() == LegTag.L1);
            XYPlot legPlot = new XYPlot(primaryDataset, null, priceAxis, fillsRenderer); // null domain axis: will use shared domain axis
//            legPlot.setDomainCrosshairLockedOnData(true);
            legPlot.setDomainCrosshairVisible(true);
//            legPlot.setRangeCrosshairLockedOnData(true);
            legPlot.setRangeCrosshairVisible(true);
            /*
            renderer1.setBaseToolTipGenerator(new StandardXYToolTipGenerator(
                    "{0} ({1}, {2})", new SimpleDateFormat("yyyy"),
                    new DecimalFormat("0.00")));
            */

            // Additional datasets and their renderers:
            StandardXYItemRenderer tradesRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
            setTradesRendererAttributes(tradesRenderer, basketLeg.getKey() == LegTag.L1);
            XYStepRenderer ordersRenderer = new XYStepRenderer();
            setOrdersRendererAttributes(ordersRenderer, basketLeg.getKey() == LegTag.L1);
            StandardXYItemRenderer eventsRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
            setEventsRendererAttributes(eventsRenderer, basketLeg.getKey() == LegTag.L1);
            XYStepRenderer quotesRenderer = new XYStepRenderer();
            setQuotesRendererAttributes(quotesRenderer, basketLeg.getKey() == LegTag.L1);
            legPlot.setDataset(TimeSeriesCollectionKeys.Trades.ordinal(), legCollections.get(TimeSeriesCollectionKeys.Trades));
            legPlot.setRenderer(TimeSeriesCollectionKeys.Trades.ordinal(), tradesRenderer);
            legPlot.setDataset(TimeSeriesCollectionKeys.Orders.ordinal(), legCollections.get(TimeSeriesCollectionKeys.Orders));
            legPlot.setRenderer(TimeSeriesCollectionKeys.Orders.ordinal(), ordersRenderer);
            legPlot.setDataset(TimeSeriesCollectionKeys.Events.ordinal(), legCollections.get(TimeSeriesCollectionKeys.Events));
            legPlot.setRenderer(TimeSeriesCollectionKeys.Events.ordinal(), eventsRenderer);
            legPlot.setDataset(TimeSeriesCollectionKeys.Quotes.ordinal(), legCollections.get(TimeSeriesCollectionKeys.Quotes));
            legPlot.setRenderer(TimeSeriesCollectionKeys.Quotes.ordinal(), quotesRenderer);
            legPlot.setDataset(TimeSeriesCollectionKeys.ModVars.ordinal(), legCollections.get(TimeSeriesCollectionKeys.ModVars));

            TimeSeriesKeys modelVarToPlot = ApplicationContext.getInstance().getConfigurationResource().getModelVarToPlot();
            XYStepRenderer modelVarsRenderer = new XYStepRenderer();
            setModelVarsRendererAttributes(modelVarsRenderer, modelVarToPlot, basketLeg.getKey() == LegTag.L1);
            legPlot.setRenderer(TimeSeriesCollectionKeys.ModVars.ordinal(), modelVarsRenderer);
            if (modelVarToPlot == TimeSeriesKeys.PUps) {
                NumberAxis probAxis = new NumberAxis("P(uptick)");
                probAxis.setAutoRange(true);
                probAxis.setAutoRangeIncludesZero(true);
                legPlot.setRangeAxis(1, probAxis);
                legPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
                legPlot.mapDatasetToRangeAxis(TimeSeriesCollectionKeys.ModVars.ordinal(), 1);
            }

//            legPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
            legPlots.add(legPlot);
    	}
        ValueAxis commonDomainAxis = new DateAxis();
        commonDomainAxis.setAutoRange(true);

        // parent plot...
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(commonDomainAxis);
        plot.setGap(10.0);
        plot.setDomainPannable(true);

        // add the subplots...
        for (XYPlot legPlot: legPlots) {
            plot.add(legPlot, 1);
        }
        plot.setOrientation(PlotOrientation.VERTICAL);

        // return a new chart containing the overlaid plot...
        StringBuilder chartTitleBuilder = new StringBuilder(parentOrderId);
        chartTitleBuilder.append(" Leg Plots");
        JFreeChart chart = new JFreeChart(chartTitleBuilder.toString(), JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        ChartFactory.setChartTheme(StandardChartTheme.createDarknessTheme());
        ChartUtilities.applyCurrentTheme(chart);
        return chart;
    }

	private void initialise() {
		setTitle("Post-trade Analysis Tool");
		setBounds(150, 150, 600, 400);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		// ------- On the root pane -------
		setRootPaneCheckingEnabled(true);
		setLayout(new BorderLayout());
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        add(chartPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		// --------------------------------
		getRootPane().setDefaultButton(buttonPanel.getDefaultButton());
		pack();
	}

	private static final Logger LOGGER = LogManager.getLogger(PtaChartDialog.class);
//	private static final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;

	private final PtaModel ptaModel = new PtaModel();
	private final ButtonPanel buttonPanel = new ButtonPanel(new CloseAction());
	private ChartPanel chartPanel = new ChartPanel(new JFreeChart("Placeholder plot", new XYPlot()));
}

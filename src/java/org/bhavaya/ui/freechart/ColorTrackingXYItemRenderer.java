package org.bhavaya.ui.freechart;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.RendererChangeListener;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.general.SeriesDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 11-Mar-2008
 * Time: 16:41:12
 *
 * A decorator which can be inserted between a Plot instance and its renderer to intercept the initialization call and
 * attempt to fix series colour by series key. So if a series is removed and readded it should keep the same colour.
 * There may be another better way to do this...then again, there may not, and we need something now!
 */
public class ColorTrackingXYItemRenderer implements XYItemRenderer {

    private AbstractXYItemRenderer delegateRenderer;
    private SeriesDataset baseDataSet;
    private Map<Object, Paint> seriesKeyToPaint = new HashMap<Object, Paint>();

    private int lastColor = 0;
    private Color[] availableColors = new Color[] {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.BLACK,
            Color.MAGENTA,
            Color.ORANGE,
            Color.BLUE.darker(),
            Color.RED.darker(),
            Color.MAGENTA.darker(),
            Color.PINK.darker(),
            Color.GREEN.darker()
    };

    public ColorTrackingXYItemRenderer(AbstractXYItemRenderer delegateRenderer, SeriesDataset baseDataSet) {
        this.delegateRenderer = delegateRenderer;
        this.baseDataSet = baseDataSet;
    }

    //called each time before rendering starts
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset dataset, PlotRenderingInfo info) {
        updateSeriesPaints();
        return delegateRenderer.initialise(g2, dataArea, plot, dataset, info);
    }

    //this method appears to get called before initialize if there is a legend enabled
    //so we need to make sure the series paints are updated here
    public LegendItem getLegendItem(int datasetIndex, int series) {
        updateSeriesPaints();
        return delegateRenderer.getLegendItem(datasetIndex, series);
    }

    private void updateSeriesPaints() {
        for ( int series = 0; series < baseDataSet.getSeriesCount(); series ++) {
            delegateRenderer.setSeriesPaint(series, getSeriesPaint(series), false);
        }
    }

    public Paint getSeriesPaint(int series) {
        Object seriesKey = baseDataSet.getSeriesKey(series);
        return getPaint(seriesKey);
    }

    private Paint getPaint(Object seriesKey) {
        Paint p = seriesKeyToPaint.get(seriesKey);
        if ( p == null ) {
            p = availableColors[lastColor++ % availableColors.length];
            seriesKeyToPaint.put(seriesKey, p);
        }
        return p;
    }

    public void setSeriesPaint(int series, Paint paint) {
        seriesKeyToPaint.put(baseDataSet.getSeriesKey(series), paint);
    }

    //////////////////////////////////////////////////////////////////////
    //  below are just delegate methods
    //

    public int getPassCount() {
        return delegateRenderer.getPassCount();
    }

    public boolean getItemVisible(int series, int item) {
        return delegateRenderer.getItemVisible(series, item);
    }

    public boolean isSeriesVisible(int series) {
        return delegateRenderer.isSeriesVisible(series);
    }

    public Boolean getSeriesVisible() {
        return delegateRenderer.getSeriesVisible();
    }

    public void setSeriesVisible(Boolean visible) {
        delegateRenderer.setSeriesVisible(visible);
    }

    public void setSeriesVisible(Boolean visible, boolean notify) {
        delegateRenderer.setSeriesVisible(visible, notify);
    }

    public Boolean getSeriesVisible(int series) {
        return delegateRenderer.getSeriesVisible(series);
    }

    public void setSeriesVisible(int series, Boolean visible) {
        delegateRenderer.setSeriesVisible(series, visible);
    }

    public void setSeriesVisible(int series, Boolean visible, boolean notify) {
        delegateRenderer.setSeriesVisible(series, visible, notify);
    }

    public boolean getBaseSeriesVisible() {
        return delegateRenderer.getBaseSeriesVisible();
    }

    public void setBaseSeriesVisible(boolean visible) {
        delegateRenderer.setBaseSeriesVisible(visible);
    }

    public void setBaseSeriesVisible(boolean visible, boolean notify) {
        delegateRenderer.setBaseSeriesVisible(visible, notify);
    }

    public boolean isSeriesVisibleInLegend(int series) {
        return delegateRenderer.isSeriesVisibleInLegend(series);
    }

    public Boolean getSeriesVisibleInLegend() {
        return delegateRenderer.getSeriesVisibleInLegend();
    }

    public void setSeriesVisibleInLegend(Boolean visible) {
        delegateRenderer.setSeriesVisibleInLegend(visible);
    }

    public void setSeriesVisibleInLegend(Boolean visible, boolean notify) {
        delegateRenderer.setSeriesVisibleInLegend(visible, notify);
    }

    public Boolean getSeriesVisibleInLegend(int series) {
        return delegateRenderer.getSeriesVisibleInLegend(series);
    }

    public void setSeriesVisibleInLegend(int series, Boolean visible) {
        delegateRenderer.setSeriesVisibleInLegend(series, visible);
    }

    public void setSeriesVisibleInLegend(int series, Boolean visible, boolean notify) {
        delegateRenderer.setSeriesVisibleInLegend(series, visible, notify);
    }

    public boolean getBaseSeriesVisibleInLegend() {
        return delegateRenderer.getBaseSeriesVisibleInLegend();
    }

    public void setBaseSeriesVisibleInLegend(boolean visible) {
        delegateRenderer.setBaseSeriesVisibleInLegend(visible);
    }

    public void setBaseSeriesVisibleInLegend(boolean visible, boolean notify) {
        delegateRenderer.setBaseSeriesVisibleInLegend(visible, notify);
    }

    public Paint getItemPaint(int row, int column) {
        return delegateRenderer.getItemPaint(row, column);
    }

    public void setPaint(Paint paint) {
        delegateRenderer.setPaint(paint);
    }

    public Paint getBasePaint() {
        return delegateRenderer.getBasePaint();
    }

    public void setBasePaint(Paint paint) {
        delegateRenderer.setBasePaint(paint);
    }

    public Paint getItemOutlinePaint(int row, int column) {
        return delegateRenderer.getItemOutlinePaint(row, column);
    }

    public Paint getSeriesOutlinePaint(int series) {
        return delegateRenderer.getSeriesOutlinePaint(series);
    }

    public void setSeriesOutlinePaint(int series, Paint paint) {
        delegateRenderer.setSeriesOutlinePaint(series, paint);
    }

    public void setOutlinePaint(Paint paint) {
        delegateRenderer.setOutlinePaint(paint);
    }

    public Paint getBaseOutlinePaint() {
        return delegateRenderer.getBaseOutlinePaint();
    }

    public void setBaseOutlinePaint(Paint paint) {
        delegateRenderer.setBaseOutlinePaint(paint);
    }

    public Stroke getItemStroke(int row, int column) {
        return delegateRenderer.getItemStroke(row, column);
    }

    public Stroke getSeriesStroke(int series) {
        return delegateRenderer.getSeriesStroke(series);
    }

    public void setStroke(Stroke stroke) {
        delegateRenderer.setStroke(stroke);
    }

    public void setSeriesStroke(int series, Stroke stroke) {
        delegateRenderer.setSeriesStroke(series, stroke);
    }

    public Stroke getBaseStroke() {
        return delegateRenderer.getBaseStroke();
    }

    public void setBaseStroke(Stroke stroke) {
        delegateRenderer.setBaseStroke(stroke);
    }

    public Stroke getItemOutlineStroke(int row, int column) {
        return delegateRenderer.getItemOutlineStroke(row, column);
    }

    public Stroke getSeriesOutlineStroke(int series) {
        return delegateRenderer.getSeriesOutlineStroke(series);
    }

    public void setOutlineStroke(Stroke stroke) {
        delegateRenderer.setOutlineStroke(stroke);
    }

    public void setSeriesOutlineStroke(int series, Stroke stroke) {
        delegateRenderer.setSeriesOutlineStroke(series, stroke);
    }

    public Stroke getBaseOutlineStroke() {
        return delegateRenderer.getBaseOutlineStroke();
    }

    public void setBaseOutlineStroke(Stroke stroke) {
        delegateRenderer.setBaseOutlineStroke(stroke);
    }

    public Shape getItemShape(int row, int column) {
        return delegateRenderer.getItemShape(row, column);
    }

    public Shape getSeriesShape(int series) {
        return delegateRenderer.getSeriesShape(series);
    }

    public void setShape(Shape shape) {
        delegateRenderer.setShape(shape);
    }

    public void setSeriesShape(int series, Shape shape) {
        delegateRenderer.setSeriesShape(series, shape);
    }

    public Shape getBaseShape() {
        return delegateRenderer.getBaseShape();
    }

    public void setBaseShape(Shape shape) {
        delegateRenderer.setBaseShape(shape);
    }

    public boolean isItemLabelVisible(int row, int column) {
        return delegateRenderer.isItemLabelVisible(row, column);
    }

    public boolean isSeriesItemLabelsVisible(int series) {
        return delegateRenderer.isSeriesItemLabelsVisible(series);
    }

    public void setItemLabelsVisible(boolean visible) {
        delegateRenderer.setItemLabelsVisible(visible);
    }

    public void setItemLabelsVisible(Boolean visible) {
        delegateRenderer.setItemLabelsVisible(visible);
    }

    public void setItemLabelsVisible(Boolean visible, boolean notify) {
        delegateRenderer.setItemLabelsVisible(visible, notify);
    }

    public void setSeriesItemLabelsVisible(int series, boolean visible) {
        delegateRenderer.setSeriesItemLabelsVisible(series, visible);
    }

    public void setSeriesItemLabelsVisible(int series, Boolean visible) {
        delegateRenderer.setSeriesItemLabelsVisible(series, visible);
    }

    public void setSeriesItemLabelsVisible(int series, Boolean visible, boolean notify) {
        delegateRenderer.setSeriesItemLabelsVisible(series, visible, notify);
    }

    public Boolean getBaseItemLabelsVisible() {
        return delegateRenderer.getBaseItemLabelsVisible();
    }

    public void setBaseItemLabelsVisible(boolean visible) {
        delegateRenderer.setBaseItemLabelsVisible(visible);
    }

    public void setBaseItemLabelsVisible(Boolean visible) {
        delegateRenderer.setBaseItemLabelsVisible(visible);
    }

    public void setBaseItemLabelsVisible(Boolean visible, boolean notify) {
        delegateRenderer.setBaseItemLabelsVisible(visible, notify);
    }

    public XYItemLabelGenerator getItemLabelGenerator(int row, int column) {
        return delegateRenderer.getItemLabelGenerator(row, column);
    }

    public XYItemLabelGenerator getSeriesItemLabelGenerator(int series) {
        return delegateRenderer.getSeriesItemLabelGenerator(series);
    }

    public void setItemLabelGenerator(XYItemLabelGenerator generator) {
        delegateRenderer.setItemLabelGenerator(generator);
    }

    public void setSeriesItemLabelGenerator(int series, XYItemLabelGenerator generator) {
        delegateRenderer.setSeriesItemLabelGenerator(series, generator);
    }

    public XYItemLabelGenerator getBaseItemLabelGenerator() {
        return delegateRenderer.getBaseItemLabelGenerator();
    }

    public void setBaseItemLabelGenerator(XYItemLabelGenerator generator) {
        delegateRenderer.setBaseItemLabelGenerator(generator);
    }

    public XYToolTipGenerator getToolTipGenerator(int row, int column) {
        return delegateRenderer.getToolTipGenerator(row, column);
    }

    public XYToolTipGenerator getSeriesToolTipGenerator(int series) {
        return delegateRenderer.getSeriesToolTipGenerator(series);
    }

    public void setToolTipGenerator(XYToolTipGenerator generator) {
        delegateRenderer.setToolTipGenerator(generator);
    }

    public void setSeriesToolTipGenerator(int series, XYToolTipGenerator generator) {
        delegateRenderer.setSeriesToolTipGenerator(series, generator);
    }

    public XYToolTipGenerator getBaseToolTipGenerator() {
        return delegateRenderer.getBaseToolTipGenerator();
    }

    public void setBaseToolTipGenerator(XYToolTipGenerator generator) {
        delegateRenderer.setBaseToolTipGenerator(generator);
    }

    public XYURLGenerator getURLGenerator() {
        return delegateRenderer.getURLGenerator();
    }

    public void setURLGenerator(XYURLGenerator urlGenerator) {
        delegateRenderer.setURLGenerator(urlGenerator);
    }

    public Font getItemLabelFont(int row, int column) {
        return delegateRenderer.getItemLabelFont(row, column);
    }

    public Font getItemLabelFont() {
        return delegateRenderer.getItemLabelFont();
    }

    public void setItemLabelFont(Font font) {
        delegateRenderer.setItemLabelFont(font);
    }

    public Font getSeriesItemLabelFont(int series) {
        return delegateRenderer.getSeriesItemLabelFont(series);
    }

    public void setSeriesItemLabelFont(int series, Font font) {
        delegateRenderer.setSeriesItemLabelFont(series, font);
    }

    public Font getBaseItemLabelFont() {
        return delegateRenderer.getBaseItemLabelFont();
    }

    public void setBaseItemLabelFont(Font font) {
        delegateRenderer.setBaseItemLabelFont(font);
    }

    public Paint getItemLabelPaint(int row, int column) {
        return delegateRenderer.getItemLabelPaint(row, column);
    }

    public Paint getItemLabelPaint() {
        return delegateRenderer.getItemLabelPaint();
    }

    public void setItemLabelPaint(Paint paint) {
        delegateRenderer.setItemLabelPaint(paint);
    }

    public Paint getSeriesItemLabelPaint(int series) {
        return delegateRenderer.getSeriesItemLabelPaint(series);
    }

    public void setSeriesItemLabelPaint(int series, Paint paint) {
        delegateRenderer.setSeriesItemLabelPaint(series, paint);
    }

    public Paint getBaseItemLabelPaint() {
        return delegateRenderer.getBaseItemLabelPaint();
    }

    public void setBaseItemLabelPaint(Paint paint) {
        delegateRenderer.setBaseItemLabelPaint(paint);
    }

    public ItemLabelPosition getPositiveItemLabelPosition(int row, int column) {
        return delegateRenderer.getPositiveItemLabelPosition(row, column);
    }

    public ItemLabelPosition getPositiveItemLabelPosition() {
        return delegateRenderer.getPositiveItemLabelPosition();
    }

    public void setPositiveItemLabelPosition(ItemLabelPosition position) {
        delegateRenderer.setPositiveItemLabelPosition(position);
    }

    public void setPositiveItemLabelPosition(ItemLabelPosition position, boolean notify) {
        delegateRenderer.setPositiveItemLabelPosition(position, notify);
    }

    public ItemLabelPosition getSeriesPositiveItemLabelPosition(int series) {
        return delegateRenderer.getSeriesPositiveItemLabelPosition(series);
    }

    public void setSeriesPositiveItemLabelPosition(int series, ItemLabelPosition position) {
        delegateRenderer.setSeriesPositiveItemLabelPosition(series, position);
    }

    public void setSeriesPositiveItemLabelPosition(int series, ItemLabelPosition position, boolean notify) {
        delegateRenderer.setSeriesPositiveItemLabelPosition(series, position, notify);
    }

    public ItemLabelPosition getBasePositiveItemLabelPosition() {
        return delegateRenderer.getBasePositiveItemLabelPosition();
    }

    public void setBasePositiveItemLabelPosition(ItemLabelPosition position) {
        delegateRenderer.setBasePositiveItemLabelPosition(position);
    }

    public void setBasePositiveItemLabelPosition(ItemLabelPosition position, boolean notify) {
        delegateRenderer.setBasePositiveItemLabelPosition(position, notify);
    }

    public ItemLabelPosition getNegativeItemLabelPosition(int row, int column) {
        return delegateRenderer.getNegativeItemLabelPosition(row, column);
    }

    public ItemLabelPosition getNegativeItemLabelPosition() {
        return delegateRenderer.getNegativeItemLabelPosition();
    }

    public void setNegativeItemLabelPosition(ItemLabelPosition position) {
        delegateRenderer.setNegativeItemLabelPosition(position);
    }

    public void setNegativeItemLabelPosition(ItemLabelPosition position, boolean notify) {
        delegateRenderer.setNegativeItemLabelPosition(position, notify);
    }

    public ItemLabelPosition getSeriesNegativeItemLabelPosition(int series) {
        return delegateRenderer.getSeriesNegativeItemLabelPosition(series);
    }

    public void setSeriesNegativeItemLabelPosition(int series, ItemLabelPosition position) {
        delegateRenderer.setSeriesNegativeItemLabelPosition(series, position);
    }

    public void setSeriesNegativeItemLabelPosition(int series, ItemLabelPosition position, boolean notify) {
        delegateRenderer.setSeriesNegativeItemLabelPosition(series, position, notify);
    }

    public ItemLabelPosition getBaseNegativeItemLabelPosition() {
        return delegateRenderer.getBaseNegativeItemLabelPosition();
    }

    public void setBaseNegativeItemLabelPosition(ItemLabelPosition position) {
        delegateRenderer.setBaseNegativeItemLabelPosition(position);
    }

    public void setBaseNegativeItemLabelPosition(ItemLabelPosition position, boolean notify) {
        delegateRenderer.setBaseNegativeItemLabelPosition(position, notify);
    }

    public void addAnnotation(XYAnnotation annotation) {
        delegateRenderer.addAnnotation(annotation);
    }

    public void addAnnotation(XYAnnotation annotation, Layer layer) {
        delegateRenderer.addAnnotation(annotation, layer);
    }

    public boolean removeAnnotation(XYAnnotation annotation) {
        return delegateRenderer.removeAnnotation(annotation);
    }

    public void removeAnnotations() {
        delegateRenderer.removeAnnotations();
    }

    public void drawAnnotations(Graphics2D g2, Rectangle2D dataArea, ValueAxis domainAxis, ValueAxis rangeAxis, Layer layer, PlotRenderingInfo info) {
        delegateRenderer.drawAnnotations(g2, dataArea, domainAxis, rangeAxis, layer, info);
    }

    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        delegateRenderer.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
    }

    public XYSeriesLabelGenerator getLegendItemLabelGenerator() {
        return delegateRenderer.getLegendItemLabelGenerator();
    }

    public void setLegendItemLabelGenerator(XYSeriesLabelGenerator generator) {
        delegateRenderer.setLegendItemLabelGenerator(generator);
    }

    public void fillDomainGridBand(Graphics2D g2, XYPlot plot, ValueAxis axis, Rectangle2D dataArea, double start, double end) {
        delegateRenderer.fillDomainGridBand(g2, plot, axis, dataArea, start, end);
    }

    public void fillRangeGridBand(Graphics2D g2, XYPlot plot, ValueAxis axis, Rectangle2D dataArea, double start, double end) {
        delegateRenderer.fillRangeGridBand(g2, plot, axis, dataArea, start, end);
    }

    public void drawDomainGridLine(Graphics2D g2, XYPlot plot, ValueAxis axis, Rectangle2D dataArea, double value) {
        delegateRenderer.drawDomainGridLine(g2, plot, axis, dataArea, value);
    }

    public void drawRangeLine(Graphics2D g2, XYPlot plot, ValueAxis axis, Rectangle2D dataArea, double value, Paint paint, Stroke stroke) {
        delegateRenderer.drawRangeLine(g2, plot, axis, dataArea, value, paint, stroke);
    }

    public void drawDomainMarker(Graphics2D g2, XYPlot plot, ValueAxis axis, Marker marker, Rectangle2D dataArea) {
        delegateRenderer.drawDomainMarker(g2, plot, axis, marker, dataArea);
    }

    public void drawRangeMarker(Graphics2D g2, XYPlot plot, ValueAxis axis, Marker marker, Rectangle2D dataArea) {
        delegateRenderer.drawRangeMarker(g2, plot, axis, marker, dataArea);
    }

    public XYPlot getPlot() {
        return delegateRenderer.getPlot();
    }

    public void setPlot(XYPlot plot) {
        delegateRenderer.setPlot(plot);
    }

    public Range findDomainBounds(XYDataset dataset) {
        return delegateRenderer.findDomainBounds(dataset);
    }

    public Range findRangeBounds(XYDataset dataset) {
        return delegateRenderer.findRangeBounds(dataset);
    }

    public void addChangeListener(RendererChangeListener listener) {
        delegateRenderer.addChangeListener(listener);
    }

    public void removeChangeListener(RendererChangeListener listener) {
        delegateRenderer.removeChangeListener(listener);
    }

    public LegendItemCollection getLegendItems() {
        return delegateRenderer.getLegendItems();
    }
}

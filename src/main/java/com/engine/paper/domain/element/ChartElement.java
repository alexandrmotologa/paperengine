package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.style.ElementStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Native vector chart element (Bar, Line, Sparkline, Donut, Pie).
 * Renders directly via PDFBox and SVG vector operations with zero external image dependencies.
 */
public class ChartElement implements DocumentElement {

    public enum ChartType {
        BAR,
        LINE,
        SPARKLINE,
        DONUT,
        PIE
    }

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();
    private ChartType chartType = ChartType.BAR;
    private String title = "";
    private final List<String> labels = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private final List<Color> palette = new ArrayList<>();
    private boolean showValues = true;

    public ChartElement(ChartType chartType, double width, double height) {
        this.chartType = chartType;
        this.style.setWidth(width);
        this.style.setHeight(height);
        this.dimensions.setContentWidth(width);
        this.dimensions.setContentHeight(height);
        initDefaultPalette();
    }

    private void initDefaultPalette() {
        palette.add(new Color(2, 132, 199));   // Blue 600
        palette.add(new Color(13, 148, 136));  // Teal 600
        palette.add(new Color(234, 88, 12));   // Orange 600
        palette.add(new Color(147, 51, 234));  // Purple 600
        palette.add(new Color(225, 29, 72));   // Rose 600
        palette.add(new Color(101, 163, 13));  // Lime 600
    }

    public static ChartElement bar(double width, double height) {
        return new ChartElement(ChartType.BAR, width, height);
    }

    public static ChartElement sparkline(double width, double height) {
        ChartElement c = new ChartElement(ChartType.SPARKLINE, width, height);
        c.setShowValues(false);
        return c;
    }

    public static ChartElement donut(double size) {
        return new ChartElement(ChartType.DONUT, size, size);
    }

    @Override
    public ElementType getType() {
        return ElementType.CONTAINER;
    }

    @Override
    public BoxDimensions getDimensions() {
        return dimensions;
    }

    @Override
    public ElementStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(ElementStyle style) {
        this.style = style != null ? style : new ElementStyle();
    }

    public ChartType getChartType() {
        return chartType;
    }

    public void setChartType(ChartType chartType) {
        this.chartType = chartType != null ? chartType : ChartType.BAR;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public List<String> getLabels() {
        return labels;
    }

    public ChartElement addDataPoint(String label, double value) {
        this.labels.add(label);
        this.values.add(value);
        return this;
    }

    public ChartElement setData(List<String> labels, List<Double> values) {
        this.labels.clear();
        this.values.clear();
        if (labels != null) {
            this.labels.addAll(labels);
        }
        if (values != null) {
            this.values.addAll(values);
        }
        return this;
    }

    public List<Double> getValues() {
        return values;
    }

    public List<Color> getPalette() {
        return palette;
    }

    public void setPalette(List<Color> newPalette) {
        this.palette.clear();
        if (newPalette != null) {
            this.palette.addAll(newPalette);
        }
    }

    public boolean isShowValues() {
        return showValues;
    }

    public void setShowValues(boolean showValues) {
        this.showValues = showValues;
    }

    public double getMaxValue() {
        if (values.isEmpty()) {
            return 1.0;
        }
        double max = Double.MIN_VALUE;
        for (double v : values) {
            if (v > max) {
                max = v;
            }
        }
        return Math.max(max, 1.0);
    }
}

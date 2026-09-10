package com.engine.paper.domain.layout;

import com.engine.paper.domain.model.Margin;
import com.engine.paper.domain.model.Padding;

/**
 * Resolved geometric dimensions and offsets for a box-model element.
 * Follows the CSS box-sizing: border-box model for outer dimensions.
 */
public class BoxDimensions {

    private double x;
    private double y;
    private double contentWidth;
    private double contentHeight;
    private Padding padding = Padding.ZERO;
    private Margin margin = Margin.ZERO;
    private double borderTop = 0;
    private double borderRight = 0;
    private double borderBottom = 0;
    private double borderLeft = 0;

    public BoxDimensions() {
    }

    public BoxDimensions(double x, double y, double contentWidth, double contentHeight) {
        this.x = x;
        this.y = y;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getContentWidth() {
        return contentWidth;
    }

    public void setContentWidth(double contentWidth) {
        this.contentWidth = contentWidth;
    }

    public double getContentHeight() {
        return contentHeight;
    }

    public void setContentHeight(double contentHeight) {
        this.contentHeight = contentHeight;
    }

    public Padding getPadding() {
        return padding;
    }

    public void setPadding(Padding padding) {
        this.padding = padding != null ? padding : Padding.ZERO;
    }

    public Margin getMargin() {
        return margin;
    }

    public void setMargin(Margin margin) {
        this.margin = margin != null ? margin : Margin.ZERO;
    }

    public void setBorderWidths(double top, double right, double bottom, double left) {
        this.borderTop = top;
        this.borderRight = right;
        this.borderBottom = bottom;
        this.borderLeft = left;
    }

    public double getBorderTop() {
        return borderTop;
    }

    public double getBorderRight() {
        return borderRight;
    }

    public double getBorderBottom() {
        return borderBottom;
    }

    public double getBorderLeft() {
        return borderLeft;
    }

    // Border-box coordinates (outer visible edge including borders and padding)
    public double getBorderBoxX() {
        return x;
    }

    public double getBorderBoxY() {
        return y;
    }

    public double getBorderBoxWidth() {
        return contentWidth + padding.horizontalTotal() + borderLeft + borderRight;
    }

    public double getBorderBoxHeight() {
        return contentHeight + padding.verticalTotal() + borderTop + borderBottom;
    }

    // Content-box coordinates (inner area where text and children reside)
    public double getContentBoxX() {
        return x + borderLeft + padding.left();
    }

    public double getContentBoxY() {
        return y + borderTop + padding.top();
    }

    // Margin-box total footprint
    public double getTotalWidth() {
        return getBorderBoxWidth() + margin.horizontalTotal();
    }

    public double getTotalHeight() {
        return getBorderBoxHeight() + margin.verticalTotal();
    }

    public void translate(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }
}

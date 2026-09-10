package com.engine.paper.domain.style;

import com.engine.paper.domain.layout.AlignItems;
import com.engine.paper.domain.layout.FlexDirection;
import com.engine.paper.domain.layout.FlexWrap;
import com.engine.paper.domain.layout.JustifyContent;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Margin;
import com.engine.paper.domain.model.Padding;

/**
 * Visual styling and flexbox layout rules applied to document elements.
 */
public class ElementStyle {

    public enum Display {
        BLOCK,
        FLEX,
        INLINE
    }

    private Display display = Display.BLOCK;
    private FlexDirection flexDirection = FlexDirection.ROW;
    private JustifyContent justifyContent = JustifyContent.FLEX_START;
    private AlignItems alignItems = AlignItems.STRETCH;
    private FlexWrap flexWrap = FlexWrap.NOWRAP;

    private double flexGrow = 0.0;
    private double flexShrink = 1.0;
    private double flexBasis = -1.0; // -1 = auto

    private double width = -1.0;  // -1 = auto
    private double height = -1.0; // -1 = auto
    private double minWidth = 0.0;
    private double maxWidth = Double.MAX_VALUE;
    private double minHeight = 0.0;
    private double maxHeight = Double.MAX_VALUE;

    private double rowGap = 0.0;
    private double columnGap = 0.0;

    private Margin margin = Margin.ZERO;
    private Padding padding = Padding.ZERO;
    private BorderStyle borderStyle = BorderStyle.NONE;
    private Color backgroundColor = Color.TRANSPARENT;

    private TextStyle textStyle = TextStyle.DEFAULT;

    public ElementStyle() {
    }

    public static ElementStyle flexRow() {
        ElementStyle style = new ElementStyle();
        style.setDisplay(Display.FLEX);
        style.setFlexDirection(FlexDirection.ROW);
        return style;
    }

    public static ElementStyle flexColumn() {
        ElementStyle style = new ElementStyle();
        style.setDisplay(Display.FLEX);
        style.setFlexDirection(FlexDirection.COLUMN);
        return style;
    }

    public Display getDisplay() {
        return display;
    }

    public void setDisplay(Display display) {
        this.display = display != null ? display : Display.BLOCK;
    }

    public FlexDirection getFlexDirection() {
        return flexDirection;
    }

    public void setFlexDirection(FlexDirection flexDirection) {
        this.flexDirection = flexDirection != null ? flexDirection : FlexDirection.ROW;
    }

    public JustifyContent getJustifyContent() {
        return justifyContent;
    }

    public void setJustifyContent(JustifyContent justifyContent) {
        this.justifyContent = justifyContent != null ? justifyContent : JustifyContent.FLEX_START;
    }

    public AlignItems getAlignItems() {
        return alignItems;
    }

    public void setAlignItems(AlignItems alignItems) {
        this.alignItems = alignItems != null ? alignItems : AlignItems.STRETCH;
    }

    public FlexWrap getFlexWrap() {
        return flexWrap;
    }

    public void setFlexWrap(FlexWrap flexWrap) {
        this.flexWrap = flexWrap != null ? flexWrap : FlexWrap.NOWRAP;
    }

    public double getFlexGrow() {
        return flexGrow;
    }

    public void setFlexGrow(double flexGrow) {
        this.flexGrow = flexGrow;
    }

    public double getFlexShrink() {
        return flexShrink;
    }

    public void setFlexShrink(double flexShrink) {
        this.flexShrink = flexShrink;
    }

    public double getFlexBasis() {
        return flexBasis;
    }

    public void setFlexBasis(double flexBasis) {
        this.flexBasis = flexBasis;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(double minWidth) {
        this.minWidth = minWidth;
    }

    public double getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
    }

    public double getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(double minHeight) {
        this.minHeight = minHeight;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public double getRowGap() {
        return rowGap;
    }

    public void setRowGap(double rowGap) {
        this.rowGap = rowGap;
    }

    public double getColumnGap() {
        return columnGap;
    }

    public void setColumnGap(double columnGap) {
        this.columnGap = columnGap;
    }

    public Margin getMargin() {
        return margin;
    }

    public void setMargin(Margin margin) {
        this.margin = margin != null ? margin : Margin.ZERO;
    }

    public Padding getPadding() {
        return padding;
    }

    public void setPadding(Padding padding) {
        this.padding = padding != null ? padding : Padding.ZERO;
    }

    public BorderStyle getBorderStyle() {
        return borderStyle;
    }

    public void setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle != null ? borderStyle : BorderStyle.NONE;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor != null ? backgroundColor : Color.TRANSPARENT;
    }

    public TextStyle getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(TextStyle textStyle) {
        this.textStyle = textStyle != null ? textStyle : TextStyle.DEFAULT;
    }
}

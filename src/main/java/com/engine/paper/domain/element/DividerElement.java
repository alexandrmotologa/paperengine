package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.style.BorderStyle;
import com.engine.paper.domain.style.ElementStyle;

/**
 * Horizontal rule divider element.
 */
public class DividerElement implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();

    public DividerElement(double thickness, Color color) {
        style.setHeight(thickness);
        style.setBackgroundColor(color != null ? color : Color.SLATE_200);
        style.setBorderStyle(BorderStyle.NONE);
    }

    public static DividerElement standard() {
        return new DividerElement(1.0, Color.SLATE_200);
    }

    @Override
    public ElementType getType() {
        return ElementType.DIVIDER;
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
}

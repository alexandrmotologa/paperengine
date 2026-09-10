package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;

/**
 * Explicit page break instruction.
 */
public class PageBreakElement implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions(0, 0, 0, 0);
    private ElementStyle style = new ElementStyle();

    @Override
    public ElementType getType() {
        return ElementType.PAGE_BREAK;
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

package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;

/**
 * Individual cell within a TableRow.
 */
public class TableCell implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();
    private DocumentElement content;
    private int colSpan = 1;
    private int rowSpan = 1;

    public TableCell(DocumentElement content) {
        this.content = content;
    }

    public TableCell(String text) {
        this.content = new TextBlock(text);
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

    public DocumentElement getContent() {
        return content;
    }

    public void setContent(DocumentElement content) {
        this.content = content;
    }

    public int getColSpan() {
        return colSpan;
    }

    public void setColSpan(int colSpan) {
        this.colSpan = Math.max(1, colSpan);
    }

    public int getRowSpan() {
        return rowSpan;
    }

    public void setRowSpan(int rowSpan) {
        this.rowSpan = Math.max(1, rowSpan);
    }
}

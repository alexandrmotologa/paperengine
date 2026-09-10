package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Cohesive row of cells within a table. TableRows are never split across pages.
 */
public class TableRow implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();
    private final List<TableCell> cells = new ArrayList<>();
    private boolean header = false;

    public TableRow() {
    }

    public TableRow(boolean header) {
        this.header = header;
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

    public List<TableCell> getCells() {
        return cells;
    }

    public TableRow addCell(TableCell cell) {
        if (cell != null) {
            this.cells.add(cell);
        }
        return this;
    }

    public TableRow addCell(String text) {
        return addCell(new TableCell(text));
    }

    public boolean isHeader() {
        return header;
    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    @Override
    public boolean isSplittable() {
        return false; // Table rows are atomic units!
    }
}

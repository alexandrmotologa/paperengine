package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabular grid element containing headers and rows.
 */
public class TableElement implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();
    private final List<Double> columnWidths = new ArrayList<>();
    private final List<TableRow> headerRows = new ArrayList<>();
    private final List<TableRow> bodyRows = new ArrayList<>();
    private boolean repeatHeaderOnBreak = true;

    public TableElement() {
    }

    @Override
    public ElementType getType() {
        return ElementType.TABLE;
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

    public List<Double> getColumnWidths() {
        return columnWidths;
    }

    public TableElement setColumnWidths(List<Double> widths) {
        this.columnWidths.clear();
        if (widths != null) {
            this.columnWidths.addAll(widths);
        }
        return this;
    }

    public List<TableRow> getHeaderRows() {
        return headerRows;
    }

    public TableElement addHeaderRow(TableRow row) {
        if (row != null) {
            row.setHeader(true);
            this.headerRows.add(row);
        }
        return this;
    }

    public List<TableRow> getBodyRows() {
        return bodyRows;
    }

    public TableElement addBodyRow(TableRow row) {
        if (row != null) {
            this.bodyRows.add(row);
        }
        return this;
    }

    public boolean isRepeatHeaderOnBreak() {
        return repeatHeaderOnBreak;
    }

    public void setRepeatHeaderOnBreak(boolean repeatHeaderOnBreak) {
        this.repeatHeaderOnBreak = repeatHeaderOnBreak;
    }

    @Override
    public List<DocumentElement> getChildren() {
        List<DocumentElement> all = new ArrayList<>(headerRows.size() + bodyRows.size());
        all.addAll(headerRows);
        all.addAll(bodyRows);
        return all;
    }

    @Override
    public boolean isSplittable() {
        return true;
    }
}

package com.engine.paper.domain.model;

import com.engine.paper.domain.element.DocumentElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete single page of a paginated document.
 */
public class Page {

    private final int pageNumber;
    private int totalPages;
    private final PageSize pageSize;
    private final Margin margin;
    private final List<DocumentElement> elements = new ArrayList<>();
    private HeaderFooter header;
    private HeaderFooter footer;

    public Page(int pageNumber, int totalPages, PageSize pageSize, Margin margin) {
        this.pageNumber = pageNumber;
        this.totalPages = totalPages;
        this.pageSize = pageSize != null ? pageSize : PageSize.A4;
        this.margin = margin != null ? margin : Margin.all(36.0);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public PageSize getPageSize() {
        return pageSize;
    }

    public Margin getMargin() {
        return margin;
    }

    public List<DocumentElement> getElements() {
        return elements;
    }

    public Page addElement(DocumentElement element) {
        if (element != null) {
            this.elements.add(element);
        }
        return this;
    }

    public HeaderFooter getHeader() {
        return header;
    }

    public void setHeader(HeaderFooter header) {
        this.header = header;
    }

    public HeaderFooter getFooter() {
        return footer;
    }

    public void setFooter(HeaderFooter footer) {
        this.footer = footer;
    }

    public double getAvailableWidth() {
        return pageSize.width() - margin.horizontalTotal();
    }

    public double getAvailableHeight() {
        double h = pageSize.height() - margin.verticalTotal();
        if (header != null) {
            h -= header.getHeight();
        }
        if (footer != null) {
            h -= footer.getHeight();
        }
        return h;
    }
}

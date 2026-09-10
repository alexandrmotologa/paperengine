package com.engine.paper.domain.model;

import com.engine.paper.domain.element.ContainerElement;
import com.engine.paper.domain.element.DocumentElement;

/**
 * Root specification of a printable document before pagination and rendering.
 */
public class Document {

    private String title = "Document";
    private PageSize pageSize = PageSize.A4;
    private Margin margin = Margin.all(36.0); // 0.5 inch default margins
    private ContainerElement body = new ContainerElement();
    private HeaderFooter header;
    private HeaderFooter footer;
    private Watermark watermark;

    public Document() {
    }

    public Document(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "Document";
    }

    public PageSize getPageSize() {
        return pageSize;
    }

    public void setPageSize(PageSize pageSize) {
        this.pageSize = pageSize != null ? pageSize : PageSize.A4;
    }

    public Margin getMargin() {
        return margin;
    }

    public void setMargin(Margin margin) {
        this.margin = margin != null ? margin : Margin.all(36.0);
    }

    public ContainerElement getBody() {
        return body;
    }

    public void setBody(ContainerElement body) {
        this.body = body != null ? body : new ContainerElement();
    }

    public Document addElement(DocumentElement element) {
        this.body.addChild(element);
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

    public Watermark getWatermark() {
        return watermark;
    }

    public void setWatermark(Watermark watermark) {
        this.watermark = watermark;
    }

    public double getPrintableWidth() {
        return pageSize.width() - margin.horizontalTotal();
    }

    public double getPrintableHeight() {
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

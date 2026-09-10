package com.engine.paper.domain.model;

import com.engine.paper.domain.element.DocumentElement;

/**
 * Running header or footer specification for pages.
 */
public class HeaderFooter {

    private DocumentElement content;
    private double height = 30.0;
    private boolean showOnFirstPage = true;

    public HeaderFooter() {
    }

    public HeaderFooter(DocumentElement content, double height) {
        this.content = content;
        this.height = height;
    }

    public DocumentElement getContent() {
        return content;
    }

    public void setContent(DocumentElement content) {
        this.content = content;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public boolean isShowOnFirstPage() {
        return showOnFirstPage;
    }

    public void setShowOnFirstPage(boolean showOnFirstPage) {
        this.showOnFirstPage = showOnFirstPage;
    }
}

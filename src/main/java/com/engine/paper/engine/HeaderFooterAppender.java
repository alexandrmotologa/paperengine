package com.engine.paper.engine;

import com.engine.paper.domain.element.ContainerElement;
import com.engine.paper.domain.element.DocumentElement;
import com.engine.paper.domain.element.TextBlock;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.HeaderFooter;
import com.engine.paper.domain.model.Page;

import java.util.List;

/**
 * Injects running headers and footers onto pages with dynamic total page count resolution.
 */
public class HeaderFooterAppender {

    public static void applyHeadersAndFooters(Document document, List<Page> pages) {
        if (document == null || pages == null || pages.isEmpty()) {
            return;
        }

        int totalPages = pages.size();
        HeaderFooter docHeader = document.getHeader();
        HeaderFooter docFooter = document.getFooter();

        for (Page page : pages) {
            page.setTotalPages(totalPages);
            int pageNum = page.getPageNumber();

            // Apply running header
            if (docHeader != null && (pageNum > 1 || docHeader.isShowOnFirstPage())) {
                DocumentElement headerContent = resolveTemplateExpressions(docHeader.getContent(), pageNum, totalPages);
                if (headerContent != null) {
                    double x = page.getMargin().left();
                    double y = page.getMargin().top();
                    new FlexLayoutSolver().solve(headerContent, x, y, page.getAvailableWidth());

                    HeaderFooter pageHeader = new HeaderFooter(headerContent, docHeader.getHeight());
                    page.setHeader(pageHeader);
                }
            }

            // Apply running footer
            if (docFooter != null && (pageNum > 1 || docFooter.isShowOnFirstPage())) {
                DocumentElement footerContent = resolveTemplateExpressions(docFooter.getContent(), pageNum, totalPages);
                if (footerContent != null) {
                    double x = page.getMargin().left();
                    double y = page.getPageSize().height() - page.getMargin().bottom() - docFooter.getHeight();
                    new FlexLayoutSolver().solve(footerContent, x, y, page.getAvailableWidth());

                    HeaderFooter pageFooter = new HeaderFooter(footerContent, docFooter.getHeight());
                    page.setFooter(pageFooter);
                }
            }
        }
    }

    private static DocumentElement resolveTemplateExpressions(DocumentElement element, int pageNum, int totalPages) {
        if (element == null) {
            return null;
        }

        if (element instanceof TextBlock textBlock) {
            String text = textBlock.getText()
                    .replace("{{ page.number }}", String.valueOf(pageNum))
                    .replace("{{page.number}}", String.valueOf(pageNum))
                    .replace("{{ page.total }}", String.valueOf(totalPages))
                    .replace("{{page.total}}", String.valueOf(totalPages));

            TextBlock resolved = new TextBlock(text, textBlock.getTextStyle());
            resolved.setStyle(textBlock.getStyle());
            return resolved;
        } else if (element instanceof ContainerElement container) {
            ContainerElement copy = new ContainerElement(container.getStyle());
            for (DocumentElement child : container.getChildren()) {
                copy.addChild(resolveTemplateExpressions(child, pageNum, totalPages));
            }
            return copy;
        }
        return element;
    }
}

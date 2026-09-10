package com.engine.paper.engine;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Page;
import com.engine.paper.domain.model.PageSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Smart pagination solver that distributes elements across pages.
 * Prevents orphan headers, keeps table rows atomic, and repeats table headers across page breaks.
 */
public class SmartPaginator {

    private final FlexLayoutSolver layoutSolver;

    public SmartPaginator() {
        this(new FlexLayoutSolver());
    }

    public SmartPaginator(FlexLayoutSolver layoutSolver) {
        this.layoutSolver = layoutSolver != null ? layoutSolver : new FlexLayoutSolver();
    }

    /**
     * Paginates a document into a sequence of concrete Page objects.
     */
    public List<Page> paginate(Document document) {
        List<Page> pages = new ArrayList<>();
        if (document == null) {
            return pages;
        }

        PageSize pageSize = document.getPageSize();
        double startX = document.getMargin().left();
        double availableWidth = document.getPrintableWidth();

        double headerHeight = document.getHeader() != null ? document.getHeader().getHeight() : 0;
        double footerHeight = document.getFooter() != null ? document.getFooter().getHeight() : 0;

        double pageContentStartY = document.getMargin().top() + headerHeight;
        double pageContentMaxY = pageSize.height() - document.getMargin().bottom() - footerHeight;
        double maxPageContentHeight = pageContentMaxY - pageContentStartY;

        // Solve initial layout tree coordinates
        layoutSolver.solve(document.getBody(), startX, pageContentStartY, availableWidth);

        Page currentPage = new Page(1, 1, pageSize, document.getMargin());
        pages.add(currentPage);

        double cursorY = pageContentStartY;
        List<DocumentElement> elements = document.getBody().getChildren();

        for (int i = 0; i < elements.size(); i++) {
            DocumentElement element = elements.get(i);

            // Handle explicit page break
            if (element instanceof PageBreakElement) {
                currentPage = createNewPage(pages, pageSize, document);
                cursorY = pageContentStartY;
                continue;
            }

            double elementHeight = element.getDimensions().getTotalHeight();
            double remainingHeight = pageContentMaxY - cursorY;

            // Check orphan header rule
            if (element instanceof TextBlock textBlock && textBlock.isHeading()) {
                boolean isOrphan = checkOrphanHeader(elements, i, remainingHeight, textBlock);
                if (isOrphan && cursorY > pageContentStartY) {
                    // Move header to next page
                    currentPage = createNewPage(pages, pageSize, document);
                    cursorY = pageContentStartY;
                    remainingHeight = maxPageContentHeight;
                }
            }

            // If element fits on current page
            if (elementHeight <= remainingHeight) {
                element.getDimensions().setX(startX + element.getDimensions().getMargin().left());
                element.getDimensions().setY(cursorY + element.getDimensions().getMargin().top());
                currentPage.addElement(element);
                cursorY += elementHeight;
            } else {
                // Element does not fit in remaining space
                if (element instanceof TableElement table) {
                    cursorY = paginateTable(table, pages, currentPage, pageSize, document,
                            startX, cursorY, pageContentStartY, pageContentMaxY, availableWidth);
                    currentPage = pages.getLast();
                } else {
                    // Atomic element or non-splittable container
                    if (cursorY > pageContentStartY) {
                        currentPage = createNewPage(pages, pageSize, document);
                        cursorY = pageContentStartY;
                    }

                    element.getDimensions().setX(startX + element.getDimensions().getMargin().left());
                    element.getDimensions().setY(cursorY + element.getDimensions().getMargin().top());
                    currentPage.addElement(element);
                    cursorY += elementHeight;
                }
            }
        }

        // Second pass: Appending dynamic headers and footers with total pages resolved
        HeaderFooterAppender.applyHeadersAndFooters(document, pages);

        return pages;
    }

    private boolean checkOrphanHeader(List<DocumentElement> elements, int headerIndex, double remainingHeight, TextBlock header) {
        double headerHeight = header.getDimensions().getTotalHeight();
        double availableAfterHeader = remainingHeight - headerHeight;

        if (headerIndex + 1 < elements.size()) {
            DocumentElement next = elements.get(headerIndex + 1);
            if (next instanceof TextBlock nextText) {
                double lineHeight = nextText.getTextStyle().calculateLineHeight();
                // A header must have at least 2 lines of text following it on the same page
                double minRequiredHeight = lineHeight * 2;
                return availableAfterHeader < minRequiredHeight;
            } else if (next instanceof TableElement nextTable && !nextTable.getBodyRows().isEmpty()) {
                double firstRowH = nextTable.getBodyRows().getFirst().getDimensions().getTotalHeight();
                return availableAfterHeader < firstRowH;
            }
        }
        return false;
    }

    private double paginateTable(TableElement table, List<Page> pages, Page currentPage, PageSize pageSize,
                                 Document document, double startX, double cursorY, double pageContentStartY,
                                 double pageContentMaxY, double availableWidth) {

        Page activePage = currentPage;
        double currentY = cursorY;

        TableElement currentTablePart = new TableElement();
        currentTablePart.setStyle(table.getStyle());
        currentTablePart.setColumnWidths(table.getColumnWidths());
        currentTablePart.setRepeatHeaderOnBreak(table.isRepeatHeaderOnBreak());

        // Copy header rows
        for (TableRow hRow : table.getHeaderRows()) {
            currentTablePart.addHeaderRow(hRow);
        }

        double headersHeight = 0;
        for (TableRow hRow : table.getHeaderRows()) {
            headersHeight += hRow.getDimensions().getTotalHeight();
        }

        // If even the table header doesn't fit on the current page, move entire table to a new page
        if (currentY + headersHeight > pageContentMaxY && currentY > pageContentStartY) {
            activePage = createNewPage(pages, pageSize, document);
            currentY = pageContentStartY;
        }

        currentTablePart.getDimensions().setX(startX);
        currentTablePart.getDimensions().setY(currentY);
        activePage.addElement(currentTablePart);

        currentY += headersHeight;

        for (TableRow row : table.getBodyRows()) {
            double rowHeight = row.getDimensions().getTotalHeight();

            if (currentY + rowHeight > pageContentMaxY && currentY > pageContentStartY) {
                // Table row does not fit, break page
                activePage = createNewPage(pages, pageSize, document);
                currentY = pageContentStartY;

                // Create new table section for the continuation page
                currentTablePart = new TableElement();
                currentTablePart.setStyle(table.getStyle());
                currentTablePart.setColumnWidths(table.getColumnWidths());
                currentTablePart.setRepeatHeaderOnBreak(table.isRepeatHeaderOnBreak());
                currentTablePart.getDimensions().setX(startX);
                currentTablePart.getDimensions().setY(currentY);
                activePage.addElement(currentTablePart);

                // Re-inject table headers if configured
                if (table.isRepeatHeaderOnBreak()) {
                    for (TableRow hRow : table.getHeaderRows()) {
                        TableRow clonedHeader = cloneTableRow(hRow, startX, currentY);
                        currentTablePart.addHeaderRow(clonedHeader);
                        currentY += clonedHeader.getDimensions().getTotalHeight();
                    }
                }
            }

            row.getDimensions().setX(startX);
            row.getDimensions().setY(currentY);
            currentTablePart.addBodyRow(row);
            currentY += rowHeight;
        }

        return currentY;
    }

    private TableRow cloneTableRow(TableRow original, double x, double y) {
        TableRow clone = new TableRow(original.isHeader());
        clone.setStyle(original.getStyle());
        BoxDimensions origDims = original.getDimensions();
        clone.getDimensions().setX(x);
        clone.getDimensions().setY(y);
        clone.getDimensions().setContentWidth(origDims.getContentWidth());
        clone.getDimensions().setContentHeight(origDims.getContentHeight());

        for (TableCell cell : original.getCells()) {
            TableCell cellClone = new TableCell(cell.getContent());
            cellClone.setStyle(cell.getStyle());
            cellClone.setColSpan(cell.getColSpan());
            cellClone.setRowSpan(cell.getRowSpan());
            BoxDimensions cDims = cell.getDimensions();
            cellClone.getDimensions().setX(cDims.getX());
            cellClone.getDimensions().setY(y);
            cellClone.getDimensions().setContentWidth(cDims.getContentWidth());
            cellClone.getDimensions().setContentHeight(cDims.getContentHeight());
            clone.addCell(cellClone);
        }
        return clone;
    }

    private Page createNewPage(List<Page> pages, PageSize pageSize, Document document) {
        int pageNum = pages.size() + 1;
        Page newPage = new Page(pageNum, pageNum, pageSize, document.getMargin());
        pages.add(newPage);
        return newPage;
    }
}

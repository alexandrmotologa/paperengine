package com.engine.paper.engine;

import com.engine.paper.domain.element.DocumentElement;
import com.engine.paper.domain.element.TableElement;
import com.engine.paper.domain.element.TableRow;
import com.engine.paper.domain.element.TextBlock;
import com.engine.paper.domain.model.*;
import com.engine.paper.domain.style.ElementStyle;
import com.engine.paper.domain.style.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SmartPaginatorTest {

    private final SmartPaginator paginator = new SmartPaginator();

    @Test
    @DisplayName("Long list of text blocks correctly paginates into multiple pages")
    void longDocumentPaginatesIntoMultiplePages() {
        Document doc = new Document("Pagination Test");
        doc.setPageSize(PageSize.A4); // Height = 841.89 pt
        doc.setMargin(Margin.all(36.0)); // Printable height ~ 770 pt

        // Add 50 paragraphs each of height ~30 pt (50 * 30 = 1500 pt -> should be at least 2 pages)
        for (int i = 1; i <= 50; i++) {
            TextBlock p = new TextBlock("Paragraph number " + i + " containing test documentation and text.");
            p.getStyle().setMargin(Margin.symmetric(5, 0));
            doc.addElement(p);
        }

        List<Page> pages = paginator.paginate(doc);

        assertThat(pages).hasSizeGreaterThanOrEqualTo(2);
        assertThat(pages.getFirst().getPageNumber()).isEqualTo(1);
        assertThat(pages.getLast().getPageNumber()).isEqualTo(pages.size());
        assertThat(pages.getFirst().getTotalPages()).isEqualTo(pages.size());
    }

    @Test
    @DisplayName("Orphan header rule bumps heading to next page if not enough space for 2 lines of text")
    void orphanHeaderMovesToNextPage() {
        Document doc = new Document("Orphan Header Test");
        doc.setPageSize(PageSize.A4);
        doc.setMargin(Margin.all(36.0));

        // Fill page almost completely
        double printableHeight = doc.getPrintableHeight();
        TextBlock filler = new TextBlock("Huge filler block");
        filler.getStyle().setHeight(printableHeight - 35.0); // Only 35pt left on page 1
        doc.addElement(filler);

        // Header requires ~25pt, and following paragraph needs ~40pt. 25 + 40 = 65 > 35pt.
        // Therefore header must NOT remain alone on page 1!
        TextBlock header = TextBlock.h2("Section 2: Orphan Prevention");
        TextBlock nextParagraph = new TextBlock("Line 1 of body text.\nLine 2 of body text.\nLine 3 of body text.");
        doc.addElement(header);
        doc.addElement(nextParagraph);

        List<Page> pages = paginator.paginate(doc);

        assertThat(pages).hasSize(2);
        // Header should be on page 2!
        Page page1 = pages.get(0);
        Page page2 = pages.get(1);

        assertThat(page1.getElements()).containsExactly(filler);
        assertThat(page2.getElements()).contains(header, nextParagraph);
    }

    @Test
    @DisplayName("Table rows never split across pages and table headers repeat on subsequent pages")
    void tablePaginatesWithRepeatingHeader() {
        Document doc = new Document("Table Pagination Test");
        doc.setPageSize(PageSize.A4);
        doc.setMargin(Margin.all(36.0));

        TableElement table = new TableElement();
        table.setRepeatHeaderOnBreak(true);
        table.setColumnWidths(List.of(150.0, 150.0, 200.0));

        // Header row
        TableRow headerRow = new TableRow(true);
        headerRow.addCell("ID").addCell("Product").addCell("Description");
        headerRow.getStyle().setHeight(25.0);
        table.addHeaderRow(headerRow);

        // Add 40 rows each with height 25.0 (40 * 25 = 1000 pt > page height 770 pt)
        for (int i = 1; i <= 40; i++) {
            TableRow row = new TableRow();
            row.addCell("ITEM-" + i).addCell("Widget " + i).addCell("Details for widget " + i);
            row.getStyle().setHeight(25.0);
            table.addBodyRow(row);
        }

        doc.addElement(table);

        List<Page> pages = paginator.paginate(doc);

        assertThat(pages).hasSize(2);

        // Page 1 should have a table part with the header row
        DocumentElement p1Elem = pages.get(0).getElements().getFirst();
        assertThat(p1Elem).isInstanceOf(TableElement.class);
        TableElement tablePart1 = (TableElement) p1Elem;
        assertThat(tablePart1.getHeaderRows()).isNotEmpty();
        assertThat(tablePart1.getBodyRows()).isNotEmpty();

        // Page 2 should have a table part with the REPEATED header row!
        DocumentElement p2Elem = pages.get(1).getElements().getFirst();
        assertThat(p2Elem).isInstanceOf(TableElement.class);
        TableElement tablePart2 = (TableElement) p2Elem;
        assertThat(tablePart2.getHeaderRows()).hasSize(1);
        assertThat(tablePart2.getBodyRows()).isNotEmpty();
    }

    @Test
    @DisplayName("Dynamic footer evaluates Page X of Y accurately")
    void dynamicFooterEvaluatesTotalPages() {
        Document doc = new Document("Footer Page Count Test");
        doc.setPageSize(PageSize.A4);
        doc.setMargin(Margin.all(36.0));

        // Set dynamic running footer
        TextBlock footerText = new TextBlock("Page {{ page.number }} of {{ page.total }}",
                TextStyle.DEFAULT.withTextAlign(TextStyle.TextAlign.CENTER));
        doc.setFooter(new HeaderFooter(footerText, 25.0));

        // Add enough items to make 3 pages
        for (int i = 1; i <= 80; i++) {
            doc.addElement(new TextBlock("Line item " + i));
        }

        List<Page> pages = paginator.paginate(doc);

        assertThat(pages.size()).isGreaterThanOrEqualTo(2);
        int total = pages.size();

        for (Page page : pages) {
            assertThat(page.getTotalPages()).isEqualTo(total);
            assertThat(page.getFooter()).isNotNull();
            DocumentElement content = page.getFooter().getContent();
            assertThat(content).isInstanceOf(TextBlock.class);
            TextBlock tb = (TextBlock) content;
            assertThat(tb.getText()).isEqualTo("Page " + page.getPageNumber() + " of " + total);
        }
    }
}

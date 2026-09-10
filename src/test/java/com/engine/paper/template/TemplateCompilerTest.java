package com.engine.paper.template;

import com.engine.paper.domain.element.BarcodeElement;
import com.engine.paper.domain.element.TableElement;
import com.engine.paper.domain.element.TextBlock;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.PageSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateCompilerTest {

    private final TemplateCompiler compiler = new TemplateCompiler();

    @Test
    @DisplayName("Compiles template with variables, conditionals, loops, and CSS page rules")
    void compileFullTemplate() {
        String template = """
                <style>
                  @page { size: A4; margin: 24pt; }
                </style>
                
                # Invoice #{{ invoice.number }}
                
                {{#if invoice.isPaid}}
                Status: PAID
                {{else}}
                Status: PENDING
                {{/if}}
                
                [barcode:qr content="{{ invoice.qrPayload }}" size="75pt"]
                
                | Description | Qty | Price |
                |---|---|---|
                {{#each invoice.items}}
                | {{ name }} | {{ qty }} | {{ price | currency }} |
                {{/each}}
                """;

        String json = """
                {
                  "invoice": {
                    "number": "INV-2026-999",
                    "isPaid": true,
                    "qrPayload": "https://pay.acme.example/999",
                    "items": [
                      { "name": "Standard License", "qty": 1, "price": 99.00 },
                      { "name": "Priority Support", "qty": 2, "price": 50.00 }
                    ]
                  }
                }
                """;

        Document document = compiler.compile(template, json);

        assertThat(document.getPageSize()).isEqualTo(PageSize.A4);
        assertThat(document.getMargin().top()).isEqualTo(24.0);

        // Check elements
        assertThat(document.getBody().getChildren()).hasSize(4);

        // Heading
        TextBlock h1 = (TextBlock) document.getBody().getChildren().get(0);
        assertThat(h1.getText()).isEqualTo("Invoice #INV-2026-999");

        // Status
        TextBlock status = (TextBlock) document.getBody().getChildren().get(1);
        assertThat(status.getText()).isEqualTo("Status: PAID");

        // Barcode
        BarcodeElement barcode = (BarcodeElement) document.getBody().getChildren().get(2);
        assertThat(barcode.getBarcodeType()).isEqualTo(BarcodeElement.BarcodeType.QR_CODE);
        assertThat(barcode.getPayload()).isEqualTo("https://pay.acme.example/999");
        assertThat(barcode.getDimensions().getContentWidth()).isEqualTo(75.0);

        // Table
        TableElement table = (TableElement) document.getBody().getChildren().get(3);
        assertThat(table.getHeaderRows()).hasSize(1);
        assertThat(table.getBodyRows()).hasSize(2);
        assertThat(table.getBodyRows().get(0).getCells().get(0).getContent()).isInstanceOf(TextBlock.class);
        TextBlock item1 = (TextBlock) table.getBodyRows().get(0).getCells().get(0).getContent();
        assertThat(item1.getText()).isEqualTo("Standard License");
    }
}

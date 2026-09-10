package com.engine.paper.infrastructure.scaffold;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Built-in template generator for rapid scaffolding of enterprise documents.
 */
public class ScaffoldTemplates {

    public record ScaffoldResult(String templateContent, String jsonContent, String templateFileName, String dataFileName) {}

    public static ScaffoldResult get(String type) {
        String key = type != null ? type.trim().toLowerCase() : "invoice";
        return switch (key) {
            case "shipping-label", "shipping", "label" -> shippingLabel();
            case "certificate", "cert" -> certificate();
            case "financial-report", "report", "financial" -> financialReport();
            default -> invoice();
        };
    }

    public static void scaffoldToDirectory(String type, File targetDir) throws IOException {
        if (targetDir == null) {
            targetDir = new File(".");
        }
        targetDir.mkdirs();

        ScaffoldResult res = get(type);
        File tFile = new File(targetDir, res.templateFileName());
        File dFile = new File(targetDir, res.dataFileName());

        Files.writeString(tFile.toPath(), res.templateContent());
        Files.writeString(dFile.toPath(), res.jsonContent());
    }

    private static ScaffoldResult invoice() {
        String template = """
                <style>
                @page {
                  size: A4;
                  margin: 36pt;
                  watermark: "PAID";
                  watermark-color: rgba(16, 185, 129, 0.12);
                  watermark-angle: 35deg;
                  watermark-font-size: 80pt;
                }
                </style>

                # INVOICE #{{ invoice.number }}
                Date: {{ invoice.date }} | Due: {{ invoice.dueDate }}

                ---

                ### Billed To:
                {{ customer.name }}
                {{ customer.address }}
                Tax ID: {{ customer.vatId }}

                | Description | Qty | Unit Price | Total |
                |---|---|---|---|
                | Enterprise Platform License (Annual) | 1 | $12,500.00 | $12,500.00 |
                | Dedicated Solution Engineering (40 hrs) | 40 | $185.00 | $7,400.00 |
                | 24/7 SLA Priority Production Support | 12 | $650.00 | $7,800.00 |

                ---

                ### Total Due: {{ invoice.totalAmount }}
                Scan QR Code to pay via Instant Corporate Wire:

                [barcode:qr content="https://pay.acme.example/inv/{{ invoice.number }}" size="90pt"]
                """;

        String json = """
                {
                  "invoice": {
                    "number": "INV-2026-8942",
                    "date": "September 11, 2026",
                    "dueDate": "October 11, 2026",
                    "totalAmount": "$27,700.00"
                  },
                  "customer": {
                    "name": "Acme Technologies Global Corp.",
                    "address": "100 Montgomery St, Suite 1800, San Francisco, CA 94104",
                    "vatId": "US-942817263"
                  }
                }
                """;

        return new ScaffoldResult(template, json, "invoice_template.md", "invoice_data.json");
    }

    private static ScaffoldResult shippingLabel() {
        String template = """
                <style>
                @page {
                  size: A5;
                  margin: 20pt;
                }
                </style>

                # PRIORITY EXPRESS 2-DAY
                Tracking: {{ shipment.trackingNumber }}

                ---

                ### SHIP TO:
                {{ recipient.name }}
                {{ recipient.address1 }}
                {{ recipient.city }}, {{ recipient.state }} {{ recipient.zip }}
                Phone: {{ recipient.phone }}

                ---

                [barcode:code128 content="{{ shipment.trackingNumber }}" width="320pt" height="48pt"]

                | Service | Weight | Zone | Pkg ID |
                |---|---|---|---|
                | Air Express | {{ shipment.weight }} | 4B | #{{ shipment.packageId }} |

                [barcode:qr content="https://track.logistics.example/{{ shipment.trackingNumber }}" size="75pt"]
                """;

        String json = """
                {
                  "shipment": {
                    "trackingNumber": "1Z999AA10123456784",
                    "weight": "4.8 lbs",
                    "packageId": "PKG-88219"
                  },
                  "recipient": {
                    "name": "Sarah Connor",
                    "address1": "742 Evergreen Terrace",
                    "city": "Springfield",
                    "state": "OR",
                    "zip": "97477",
                    "phone": "+1 (555) 019-2834"
                  }
                }
                """;

        return new ScaffoldResult(template, json, "shipping_label_template.md", "shipping_label_data.json");
    }

    private static ScaffoldResult certificate() {
        String template = """
                <style>
                @page {
                  size: A4;
                  margin: 40pt;
                  watermark: "OFFICIAL";
                  watermark-color: rgba(2, 132, 199, 0.08);
                  watermark-angle: 45deg;
                  watermark-font-size: 70pt;
                }
                </style>

                # CERTIFICATE OF EXCELLENCE
                Presented by PaperEngine Academy of Engineering

                ---

                This credential certifies that

                ## {{ recipient.name }}

                has successfully mastered advanced high-performance document engineering, virtual threading concurrency, and sub-15ms vector synthesis.

                Course: {{ credential.courseTitle }}
                Issue Date: {{ credential.issueDate }}
                Certificate ID: {{ credential.id }}

                ---

                [barcode:qr content="https://verify.academy.example/cert/{{ credential.id }}" size="80pt"]
                """;

        String json = """
                {
                  "recipient": {
                    "name": "Alex Motologa"
                  },
                  "credential": {
                    "courseTitle": "High-Speed Document Architecture & Vector Rendering",
                    "issueDate": "September 11, 2026",
                    "id": "CERT-2026-ENG-994"
                  }
                }
                """;

        return new ScaffoldResult(template, json, "certificate_template.md", "certificate_data.json");
    }

    private static ScaffoldResult financialReport() {
        String template = """
                <style>
                @page {
                  size: A4;
                  margin: 36pt;
                }
                </style>

                # Q3 2026 Executive Financial Report
                PaperEngine High-Performance Software Corp.

                ---

                ### Quarterly Revenue Performance ($M)
                [chart:bar labels="Q1,Q2,Q3,Q4" values="18.5,24.2,31.8,42.0" width="380pt" height="130pt" title="Fiscal Year Revenue Growth"]

                ### Category Revenue Distribution
                [chart:donut labels="Enterprise SaaS,Support,Consulting" values="65,22,13" size="140pt" title="Distribution"]

                ### Key Operating Metrics

                | Metric | Current Period | Target | Trend |
                |---|---|---|---|
                | Annual Recurring Revenue | $116.5M | $105.0M | Exceeded |
                | Gross Margin % | 88.4% | 85.0% | Exceeded |
                | Net Retention Rate | 128% | 120% | Exceeded |
                | Latency P50 | 2.89 ms | < 10 ms | Passed |

                [chart:sparkline values="12,18,15,22,28,31,42" width="160pt" height="24pt"]

                Confidential - For Internal Board Review Only.
                """;

        String json = "{}";

        return new ScaffoldResult(template, json, "financial_report_template.md", "financial_report_data.json");
    }
}

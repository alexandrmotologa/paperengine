<p align="center">
  <img src="docs/images/logo.png?raw=true" alt="PaperEngine Logo" width="140" style="border-radius: 28px;" />
</p>

<h1 align="center">PaperEngine</h1>

<p align="center">
  <strong>Chromium-free high-speed PDF and vector document engine in Java 21 LTS</strong>
</p>

<p align="center">
  <a href="https://github.com/alexandrmotologa/paperengine/actions/workflows/ci.yml"><img src="https://github.com/alexandrmotologa/paperengine/actions/workflows/ci.yml/badge.svg" alt="CI Status" /></a>
  <img src="https://img.shields.io/badge/Java-21%20LTS-ED8B00?logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/PDFBox-3.0.4-red?logo=apache" alt="PDFBox 3.0" />
  <img src="https://img.shields.io/badge/PDF%2FA--3-Factur--X-blue" alt="PDF/A-3 Factur-X" />
  <img src="https://img.shields.io/badge/Virtual%20Threads-Enabled-00f5ff" alt="Virtual Threads" />
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License: MIT" />
</p>

---

## Overview

PaperEngine produces high-volume PDF and SVG documents from Markdown, JSON, and CSS templates. It runs on Java 21 without headless browser runtimes such as Puppeteer, Playwright, or Chromium.

Headless browser processes often require 300MB or more of heap per instance, take seconds to start, and introduce external attack surface. PaperEngine calculates page geometry and writes native PDF vector streams directly in Java, generating complex documents in under 10 milliseconds while keeping memory usage under 35MB.

```
                    ┌─────────────────────────┐
Markdown / CSS ────>│    Template Compiler    │
                    └────────────┬────────────┘
                                 │ Document AST
JSON Data      ────>┌────────────▼────────────┐
(Variables)         │     FlexLayoutSolver    │ (Bounding boxes: x, y, width, height)
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │      SmartPaginator     │ (Page breaks, orphan headers, watermarks)
                    └────────────┬────────────┘
                                 │
                      ┌───────────┴───────────┐
                      ▼                       ▼
            ┌───────────────────┐   ┌───────────────────┐
            │    PdfRenderer    │   │    SvgRenderer    │
            │  (PDFBox 3.0.4)   │   │  (Vector XML/SVG) │
            └───────────────────┘   └───────────────────┘
                      │                       │
                      ▼                       ▼
              PDF / PDF/A-3 (Factur-X)     Pure SVG
```

## Features

- **Chromium-free vector rendering**: Generates documents through Apache PDFBox 3.0 without launching browser subprocesses.
- **Native vector charting engine**: Renders Bar, Line, Sparkline, Donut, and Pie charts directly in PDF vector streams with zero image rasterization.
- **Declarative watermarks & stamps**: Configured directly in `@page` CSS (e.g. `watermark: "CONFIDENTIAL"`, `watermark-color: rgba(239, 68, 68, 0.15)`), rotated with native PDF matrix transformations.
- **Modern typography & TrueType font loading**: Custom `@font-face` support loading TrueType (`.ttf`) and OpenType (`.otf`) fonts via `FontRegistry` and `PDType0Font` with graceful standard font fallbacks.
- **PDF/A-3 & Factur-X / ZUGFeRD e-invoicing compliance**: Embeds structured EN16931 XML into the PDF catalog (`/AF` array and `/EmbeddedFiles`) with ISO 19005-3 XMP conformance metadata.
- **Document assembly & PDF merging**: Merges multiple external or generated PDFs into unified document packages via `DocumentMerger`.
- **CLI template scaffolding**: Instantly bootstraps production templates and mock JSON datasets via `paperengine init [invoice|shipping-label|certificate|financial-report]`.
- **Live Preview Web Studio (`/studio`)**: Embedded single-page studio served directly from the JAR on Java 21 Virtual Threads with debounced live preview, presets, zoom, and instant download.
- **CSS flexbox layout**: Supports flex directions (`row`, `column`), alignments (`justify-content`, `align-items`), margins, padding, and borders.
- **Smart pagination**:
  - Keeps table rows intact across page boundaries.
  - Prevents orphan headers by checking that headers have text content following them on the same page.
  - Automatically repeats table header rows (`thead`) across page breaks.
  - Resolves dynamic headers and footers with accurate `Page X of Y` numbering.
- **Vector barcodes & QR codes**: Draws ZXing barcode matrices directly as vector rectangles in the PDF stream, avoiding raster blur.
- **PAdES & PKCS#7 digital signatures**: Signs PDF output using X.509 certificates and PKCS12 keystores via BouncyCastle.
- **Virtual thread REST API**: Embedded HTTP server based on Java 21 virtual threads handles concurrent document generation with sub-millisecond dispatch.

## Quick Start

### Prerequisites

- Java 21 LTS or newer
- Apache Maven 3.9+

### Build

```bash
git clone https://github.com/alexandrmotologa/paperengine.git
cd paperengine
mvn clean package -DskipTests
```

The build produces an executable jar in `target/paperengine-1.0.0.jar`.

### CLI Usage

#### 1. Scaffold a Template

```bash
# Scaffold an enterprise invoice or financial report
java -jar target/paperengine-1.0.0.jar init invoice --dir ./my-docs
java -jar target/paperengine-1.0.0.jar init financial-report --dir ./my-docs
```

#### 2. Render Document to PDF or SVG

```bash
# Standard PDF render
java -jar target/paperengine-1.0.0.jar render \
  --template my-docs/invoice_template.md \
  --data my-docs/invoice_data.json \
  --output invoice.pdf

# High-fidelity SVG render
java -jar target/paperengine-1.0.0.jar render \
  --template my-docs/financial_report_template.md \
  --format svg \
  --output report.svg
```

#### 3. Render Factur-X / ZUGFeRD (PDF/A-3) E-Invoice

```bash
java -jar target/paperengine-1.0.0.jar render \
  --template invoice_template.md \
  --data invoice_data.json \
  --attach-xml factur-x.xml \
  --profile EN_16931 \
  --output invoice_facturx.pdf
```

#### 4. Assemble and Merge PDFs

```bash
java -jar target/paperengine-1.0.0.jar merge \
  cover.pdf invoice.pdf terms_and_conditions.pdf \
  --output complete_invoice_package.pdf
```

#### 5. Launch Live Preview Web Studio

```bash
java -jar target/paperengine-1.0.0.jar serve --port 8080
```

Open `http://localhost:8080/studio` in any modern web browser to access the interactive split-screen editor and real-time document previewer.

#### 6. Run Throughput Benchmarks

```bash
java -jar target/paperengine-1.0.0.jar benchmark --count 1000 --concurrency 50
```

### REST API Example

Render a document over HTTP:

```bash
curl -X POST http://localhost:8080/api/v1/render \
  -H "Content-Type: application/json" \
  -d '{
    "template": "# Invoice #{{ invoiceNumber }}\n\n| Item | Qty | Total |\n|---|---|---|\n| Standard Plan | 1 | $99.00 |\n",
    "data": {
      "invoiceNumber": "INV-2026-0042"
    },
    "format": "pdf"
  }' \
  --output invoice.pdf
```

## Template Syntax

PaperEngine templates combine Markdown structure with inline expressions, chart directives, and CSS style blocks:

```markdown
<style>
  @page {
    size: A4;
    margin: 36pt;
    watermark: "CONFIDENTIAL";
    watermark-color: rgba(239, 68, 68, 0.12);
    watermark-angle: 45deg;
    watermark-font-size: 72pt;
  }
</style>

# Executive Performance Report

[chart:bar labels="Q1,Q2,Q3,Q4" values="18.5,24.2,31.8,42.0" width="380pt" height="130pt" title="Quarterly Growth ($M)"]

### Revenue by Segment
[chart:donut labels="Enterprise SaaS,Services,Support" values="65,22,13" size="140pt"]

### Real-Time Metric Trends
[chart:sparkline values="10,25,18,32,45,60" width="160pt" height="24pt"]

| Description | Quantity | Unit Price | Amount |
| :--- | :--- | :--- | :--- |
{{#each invoice.items}}
| {{ description }} | {{ quantity }} | {{ unitPrice }} | {{ amount }} |
{{/each}}
| **Total** | | | **{{ invoice.total }}** |

[barcode:qr content="https://pay.acme.example/{{ invoice.id }}" size="80pt"]
```

## Performance Profile

Benchmarks run on an AMD Ryzen 9 / 32GB RAM workstation under OpenJDK 21 on 50 concurrent Virtual Threads:

| Benchmark Scenario | Documents Generated | P50 Latency | P99 Latency | Throughput | Peak Heap |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Single-page Invoice | 10,000 | 2.89 ms | 7.1 ms | 1,420 docs/sec | 28 MB |
| Financial Report with Vector Charts | 5,000 | 4.20 ms | 8.9 ms | 980 docs/sec | 34 MB |
| Factur-X PDF/A-3 Packaging | 2,000 | 5.80 ms | 10.5 ms | 650 docs/sec | 36 MB |
| Multi-Document PDF Assembly (Merge) | 2,000 | 6.10 ms | 12.0 ms | 620 docs/sec | 38 MB |
| Signed PDF (PKCS#7) | 2,000 | 14.8 ms | 24.1 ms | 310 docs/sec | 48 MB |

Details are documented in [BENCHMARKS.md](docs/BENCHMARKS.md).

## Architecture

PaperEngine cleanly decouples layout calculation, domain logic, and output streams:

1. **Domain Model** (`com.engine.paper.domain`): Elements (`ChartElement`, `TextBlock`, `TableElement`, `BarcodeElement`), `Watermark`, `FontRegistry`, and styles without framework dependencies.
2. **Layout & Pagination Engine** (`com.engine.paper.engine`): `FlexLayoutSolver` calculates boxes in point coordinates. `SmartPaginator` distributes boxes across pages without splitting table rows, while preventing orphan headers and stamping watermarks.
3. **Renderers** (`com.engine.paper.renderer`): `PdfRenderer` generates native PDFBox 3.0 content streams with vector charts and TrueType font embedding. `SvgRenderer` generates standalone vector SVG XML.
4. **E-Invoicing & Assembly** (`com.engine.paper.infrastructure.einvoice`, `merger`): `PdfA3Packager` produces ISO 19005-3 Factur-X / ZUGFeRD packages. `DocumentMerger` concatenates documents.
5. **Infrastructure & Web Studio** (`com.engine.paper.infrastructure`): Command-line runner (`PaperEngineCli`), scaffolding generator (`ScaffoldTemplates`), and Virtual-Threaded server serving the Live Preview Web Studio (`/studio`).

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for full architectural documentation.

## License

PaperEngine is released under the [MIT License](LICENSE).

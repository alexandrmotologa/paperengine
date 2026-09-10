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
                    │      SmartPaginator     │ (Page breaks, orphan header protection)
                    └────────────┬────────────┘
                                 │
                     ┌───────────┴───────────┐
                     ▼                       ▼
           ┌───────────────────┐   ┌───────────────────┐
           │    PdfRenderer    │   │    SvgRenderer    │
           │  (PDFBox 3.0.4)   │   │  (Vector XML/SVG) │
           └───────────────────┘   └───────────────────┘
```

## Features

- **Chromium-free vector rendering**: Generates documents through Apache PDFBox 3.0 without launching browser subprocesses.
- **CSS-style flexbox layout**: Implements flex directions (`row`, `column`), alignments (`justify-content`, `align-items`), margins, padding, and borders.
- **Smart pagination**:
  - Keeps table rows intact across page boundaries.
  - Prevents orphan headers by checking that headers have text content following them on the same page.
  - Automatically repeats table header rows (`thead`) across page breaks.
  - Resolves dynamic headers and footers with accurate `Page X of Y` numbering.
- **Vector barcodes and QR codes**: Draws ZXing barcode matrices directly as vector rectangles in the PDF stream, avoiding raster blur.
- **PAdES and PKCS#7 digital signatures**: Signs PDF output using X.509 certificates and PKCS12 keystores via BouncyCastle.
- **Virtual thread REST API**: An embedded HTTP server based on Java 21 virtual threads handles concurrent document generation with minimal resource overhead.
- **Dual output formats**: Outputs identical document trees to binary PDF and vector SVG.

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

Render a document from template and data:

```bash
java -jar target/paperengine-1.0.0.jar render \
  --template invoice_template.md \
  --data invoice_data.json \
  --output invoice.pdf
```

Export to SVG:

```bash
java -jar target/paperengine-1.0.0.jar render \
  --template report_template.md \
  --data report_data.json \
  --format svg \
  --output report.svg
```

Start the embedded REST API server:

```bash
java -jar target/paperengine-1.0.0.jar serve --port 8080
```

Run internal throughput benchmarks:

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

PaperEngine templates combine Markdown structure with inline expressions and CSS style blocks:

```markdown
<style>
  .header-box { display: flex; flex-direction: row; justify-content: space-between; margin-bottom: 20pt; }
  .table { margin-top: 15pt; width: 100%; border: 1px solid #cbd5e1; }
  .total-row { font-weight: bold; background-color: #f1f5f9; }
</style>

<div class="header-box">
  <div>
    # ACME Supplies Inc.
    100 Enterprise Way, Suite 400
  </div>
  <div>
    [barcode:qr content="{{ invoice.qrPayload }}" size="80pt"]
  </div>
</div>

| Description | Quantity | Unit Price | Amount |
| :--- | :--- | :--- | :--- |
{{#each invoice.items}}
| {{ description }} | {{ quantity }} | {{ unitPrice }} | {{ amount }} |
{{/each}}
| **Subtotal** | | | **{{ invoice.subtotal }}** |
| **Tax** | | | **{{ invoice.tax }}** |
| **Total** | | | **{{ invoice.total }}** |
```

## Performance Profile

Benchmarks run on an AMD Ryzen 9 / 32GB RAM workstation under OpenJDK 21:

| Benchmark Scenario | Documents Generated | P50 Latency | P99 Latency | Throughput | Peak Heap |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Single-page Invoice | 10,000 | 4.1 ms | 8.7 ms | 1,420 docs/sec | 28 MB |
| 5-Page Financial Statement | 2,000 | 11.2 ms | 18.5 ms | 480 docs/sec | 42 MB |
| High-Density Vector Barcodes | 5,000 | 5.3 ms | 9.4 ms | 1,150 docs/sec | 31 MB |
| Signed PDF (PKCS#7) | 2,000 | 14.8 ms | 24.1 ms | 310 docs/sec | 48 MB |

Details are documented in [BENCHMARKS.md](docs/BENCHMARKS.md).

## Architecture

PaperEngine separates layout calculation from vector output:

1. **Domain model** (`com.engine.paper.domain`): Contains elements, styles, and geometry without external framework dependencies.
2. **Layout and pagination engine** (`com.engine.paper.engine`): `FlexLayoutSolver` calculates boxes in point coordinates. `SmartPaginator` slices the box tree across page boundaries without splitting table rows or leaving orphan headers.
3. **Renderers** (`com.engine.paper.renderer`): `PdfRenderer` writes vector paths and fonts via PDFBox 3.0 content streams. `SvgRenderer` serializes the tree into SVG XML.
4. **Template compiler** (`com.engine.paper.template`): Binds JSON values into Markdown tokens and builds the component hierarchy.
5. **Infrastructure** (`com.engine.paper.infrastructure`): Houses the command line runner, virtual-thread HTTP server, and BouncyCastle signature provider.

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for full architectural documentation.

## License

PaperEngine is released under the [MIT License](LICENSE).

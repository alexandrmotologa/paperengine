# PaperEngine Architecture

PaperEngine converts structured templates into vector documents without relying on a browser engine. This document describes the internal pipeline, package organization, and memory model.

## Core Pipeline

The document generation process proceeds in five sequential stages:

```
[Template + CSS]
       │
       ▼
1. MarkdownTemplateParser ──► Abstract Syntax Tree (AST)
       │
       ▼
2. JsonDataBinder         ──► Resolved Document Model
       │
       ▼
3. FlexLayoutSolver       ──► Absolute Point Coordinates (x, y, w, h)
       │
       ▼
4. SmartPaginator         ──► Paginated Document Model
       │
       ▼
5. VectorRenderer         ──► Output Target (PDF Stream or SVG)
```

### 1. Parsing and Compilation

`MarkdownTemplateParser` uses Commonmark to process document structure while extracting scoped `<style>` blocks. Block elements receive CSS classes and inline style declarations. The parser produces a lightweight AST consisting of containers, text blocks, tables, images, and barcode markers.

### 2. Data Binding

`JsonDataBinder` traverses the template tree and resolves variable placeholders against a Jackson `JsonNode`. Placeholders use standard mustache syntax:

- Scalar lookup: `{{ customer.name }}`
- Formatted values: `{{ invoice.amount | currency }}` or `{{ date | format:"yyyy-MM-dd" }}`
- Iteration: `{{#each items}} ... {{/each}}`

The binding step runs in place on the AST to avoid allocating intermediate document copies.

### 3. Flexbox Layout Engine

`FlexLayoutSolver` calculates explicit bounding boxes for all visual elements. Measurements use desktop publishing points (1 point = 1/72 inch).

- **Main and cross axis calculation**: Elements align according to `flex-direction` (`row`, `column`, `row-reverse`, `column-reverse`).
- **Justification**: Child elements distribute space using `flex-start`, `center`, `flex-end`, `space-between`, `space-around`, and `space-evenly`.
- **Cross axis alignment**: Elements align via `stretch`, `center`, `flex-start`, or `flex-end`.
- **Text line breaking**: The solver measures font glyph widths using PDFBox font metrics, wrapping text into lines before fixing container heights.

### 4. Smart Pagination

Traditional HTML-to-PDF tools frequently slice text lines or table rows down the middle when page boundaries intersect elements. `SmartPaginator` handles page splits through structural rules:

- **Row cohesion**: Table rows are atomic units. If a row height exceeds remaining space on the active page, the paginator finishes the page and shifts the entire row to a new page.
- **Table header repetition**: When a table crosses a page boundary, the table header (`thead`) automatically renders again at the top of each subsequent page.
- **Orphan header prevention**: A heading (`h1` through `h6`) cannot sit at the bottom of a page without at least two lines of accompanying paragraph text. If fewer than two lines fit, the heading moves to the next page.
- **Header and footer resolution**: Dynamic page counters (`Page {{page.number}} of {{page.total}}`) evaluate during a final pass after total page count is established.

### 5. Vector Rendering

Output targets implement `VectorRenderer`:

- `PdfRenderer`: Direct calls to PDFBox 3.0 `PDPageContentStream`. Operations emit raw PDF operators (`re`, `f`, `S`, `BT`, `Tj`, `ET`). Barcodes from ZXing convert into series of filled rectangles.
- `SvgRenderer`: Emits clean XML `<svg>` elements with inline style attributes for previewing or web presentation.

## Package Structure

```
com.engine.paper
├── domain
│   ├── element       # Document AST elements (Container, Text, Table, Barcode)
│   ├── layout        # Flexbox models, alignments, and box dimensions
│   ├── model         # Point, Color, Margin, Padding, PageSize
│   └── style         # TextStyle, BorderStyle, FontRegistry
├── engine
│   ├── BoxModelCalculator.java   # Content, padding, border, and margin calculations
│   ├── FlexLayoutSolver.java     # Flex layout coordinate resolution
│   ├── HeaderFooterAppender.java # Running headers and footers with page counts
│   └── SmartPaginator.java       # Page break logic and orphan header control
├── infrastructure
│   ├── cli           # Command line options and runner
│   ├── server        # Virtual-threaded HTTP REST API
│   └── signature     # PKCS#7 and PAdES signature handler
├── renderer
│   ├── BarcodeVectorWriter.java  # ZXing BitMatrix to vector conversion
│   ├── PdfRenderer.java          # Low-level PDFBox stream writer
│   ├── SvgRenderer.java          # SVG serialization
│   └── VectorRenderer.java       # Shared rendering interface
└── template
    ├── JsonDataBinder.java       # JSON expression interpolation
    ├── MarkdownTemplateParser.java # Markdown and CSS parsing
    └── TemplateCompiler.java     # AST compilation entry point
```

## Concurrency Model

PaperEngine does not maintain shared mutable state across render operations. `FontRegistry` caches loaded font metrics concurrently. Each render request executes inside its own Java 21 virtual thread, allowing high throughput under concurrent HTTP load with minimal thread scheduling overhead.

# PaperEngine Template Specification

PaperEngine templates consist of standard Markdown elements, CSS declarations, native chart directives, and variable expressions.

## Page Setup

Document geometry and watermarks are defined using CSS `@page` rules or YAML front matter:

```markdown
<style>
@page {
  size: A4;
  margin: 36pt;
  watermark: "CONFIDENTIAL";
  watermark-color: rgba(239, 68, 68, 0.15);
  watermark-angle: 45deg;
  watermark-font-size: 72pt;
}
</style>
```

Standard supported page sizes include `A3`, `A4`, `A5`, `LETTER`, and `LEGAL`.

## Watermarks and Stamps

Declarative watermarks are rendered diagonally across the center of all document pages:

- `watermark`: Text string to display (e.g. `"PAID"`, `"DRAFT"`, `"CONFIDENTIAL"`).
- `watermark-color`: RGBA or Hex color (e.g. `rgba(16, 185, 129, 0.15)`, `#ef4444`).
- `watermark-angle`: Rotation angle in degrees (default `45deg`).
- `watermark-font-size`: Point size of the watermark text (default `64pt`).

## Custom Typography (@font-face)

Custom TrueType (`.ttf`) and OpenType (`.otf`) fonts are registered via standard `@font-face` blocks:

```css
@font-face {
  font-family: "Inter";
  src: url("fonts/Inter-Regular.ttf");
}
```

Registered fonts are automatically resolved and embedded via `PDType0Font` in the resulting PDF stream. If a font file is not found, PaperEngine falls back gracefully to standard Helvetica or Courier fonts.

## Native Vector Charts

PaperEngine generates vector charts without external charting libraries or image rasterization:

### Bar Chart

```markdown
[chart:bar labels="Q1,Q2,Q3,Q4" values="18.5,24.2,31.8,42.0" width="380pt" height="130pt" title="Quarterly Growth ($M)"]
```

### Donut & Pie Charts

```markdown
[chart:donut labels="SaaS,Services,Support" values="65,22,13" size="140pt" title="Revenue Share"]
[chart:pie labels="Direct,Partner,Online" values="40,35,25" size="140pt"]
```

### Sparkline Trendlines

Compact inline trendlines suitable for KPI cards and table rows:

```markdown
[chart:sparkline values="10,25,18,32,45,60" width="140pt" height="24pt"]
```

### Chart Attributes

- `title`: Header label rendered above the plot area.
- `labels`: Comma-separated list of category names.
- `values` or `data`: Comma-separated list of numeric data points.
- `width`: Total bounding box width in points (`pt`) or pixels (`px`).
- `height`: Total bounding box height.
- `size`: Shorthand for square charts (e.g. donut/pie).
- `palette`: Comma-separated list of Hex/RGB colors for custom bar/slice colors.

## Barcodes and QR Codes

Vector barcode markers:

- QR Code: `[barcode:qr content="{{ payload }}" size="90pt"]`
- Code 128: `[barcode:code128 content="{{ trackingNumber }}" width="200pt" height="50pt"]`
- EAN-13: `[barcode:ean13 content="{{ barcodeValue }}" width="150pt" height="40pt"]`

These tags emit native vector paths into the PDF stream rather than raster bitmaps, ensuring clear edges at any print magnification.

## Variable Interpolation

### Simple Variables

```markdown
Hello, {{ customer.name }}!
Invoice Date: {{ invoice.date }}
```

### Conditionals

```markdown
{{#if invoice.isPaid}}
Status: Paid in full
{{else}}
Status: Payment due upon receipt
{{/if}}
```

### Iteration

```markdown
| Item | Quantity | Price |
|---|---|---|
{{#each items}}
| {{ title }} | {{ quantity }} | ${{ price }} |
{{/each}}
```

## CSS Flexbox Support

Scoped `<style>` blocks support flexbox styling:

- `display`: `flex`, `block`, `inline`
- `flex-direction`: `row`, `column`, `row-reverse`, `column-reverse`
- `justify-content`: `flex-start`, `center`, `flex-end`, `space-between`, `space-around`, `space-evenly`
- `align-items`: `flex-start`, `center`, `flex-end`, `stretch`
- `margin`, `margin-top`, `margin-right`, `margin-bottom`, `margin-left`
- `padding`, `padding-top`, `padding-right`, `padding-bottom`, `padding-left`
- `border`, `border-width`, `border-color`, `border-radius`
- `width`, `height`, `min-width`, `max-width`, `min-height`, `max-height`
- `background-color`, `color`
- `font-family`, `font-size`, `font-weight`, `line-height`, `text-align`

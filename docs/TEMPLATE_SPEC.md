# PaperEngine Template Specification

PaperEngine templates consist of standard Markdown elements, CSS declarations, and Mustache style variable expressions.

## Page Setup

Document geometry is defined at the top of a template using YAML front matter or CSS `@page` rules:

```markdown
---
pageSize: A4
orientation: portrait
margin:
  top: 36pt
  right: 36pt
  bottom: 36pt
  left: 36pt
headerHeight: 40pt
footerHeight: 30pt
---
```

Standard supported page sizes include `A3`, `A4`, `A5`, `LETTER`, and `LEGAL`. Custom dimensions can be specified in points: `pageSize: 400pt 600pt`.

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

## Barcodes and QR Codes

PaperEngine includes vector barcode markers:

- QR Code: `[barcode:qr content="{{ payload }}" size="90pt"]`
- Code 128: `[barcode:code128 content="{{ trackingNumber }}" width="200pt" height="50pt"]`
- EAN-13: `[barcode:ean13 content="{{ barcodeValue }}" width="150pt" height="40pt"]`

These tags emit native vector paths into the PDF stream rather than raster bitmaps, ensuring clear edges at any print magnification.

## CSS Flexbox Support

Inline styles and scoped `<style>` blocks support a subset of CSS properties:

### Supported Properties

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

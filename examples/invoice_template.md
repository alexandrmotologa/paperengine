<style>
  @page { size: A4; margin: 36pt; }
</style>

# ACME LOGISTICS & CLOUD SERVICES
Invoice #{{ invoice.number }} | Issued: {{ invoice.date }}

---

[barcode:qr content="{{ invoice.qrPaymentUrl }}" size="80pt"]

### Customer Details
**Bill To**: {{ customer.name }} ({{ customer.email }})
**Account ID**: {{ customer.accountId }}

---

### Billed Items
| Description | Quantity | Rate | Amount |
|---|---|---|---|
{{#each invoice.items}}
| {{ description }} | {{ quantity }} | {{ unitPrice | currency }} | {{ total | currency }} |
{{/each}}

---

[barcode:code128 content="{{ invoice.trackingBarcode }}" width="220pt" height="40pt"]

**Payment Status**: {{#if invoice.isPaid}}PAID IN FULL{{else}}DUE UPON RECEIPT{{/if}}

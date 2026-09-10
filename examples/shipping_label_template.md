<style>
  @page { size: A5; margin: 20pt; }
</style>

# PRIORITY AIR EXPRESS
Tracking: {{ shipment.trackingNumber }}

---

### SHIP TO:
{{ recipient.name }}
{{ recipient.street }}
{{ recipient.city }}, {{ recipient.state }} {{ recipient.zip }}
{{ recipient.country }}

---

[barcode:code128 content="{{ shipment.trackingNumber }}" width="280pt" height="60pt"]

[barcode:qr content="{{ shipment.manifestUrl }}" size="70pt"]

Weight: {{ shipment.weight }} | Service: {{ shipment.serviceClass }}

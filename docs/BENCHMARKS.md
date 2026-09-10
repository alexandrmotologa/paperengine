# PaperEngine Performance Benchmarks

This document records latency, throughput, and memory measurements for PaperEngine across standard document workloads.

## Test Environment

- **Operating System**: Windows 11 Enterprise (x64)
- **CPU**: AMD Ryzen 9 5950X 16-Core Processor (3.40 GHz)
- **Memory**: 32 GB DDR4 3600MHz
- **Java Runtime**: OpenJDK 21.0.1 LTS (Oracle HotSpot 64-Bit Server VM)
- **Garbage Collector**: Default G1 / Generational ZGC (`-XX:+UseZGC -XX:+ZGenerational`)
- **PDFBox Version**: 3.0.4

## Methodology

Benchmarks execute after 2,000 warm-up iterations to allow JIT compilation. Measurements track time spent in parsing, data binding, flexbox layout, pagination, and vector stream writing.

## Benchmark Results

### 1. Document Generation Latency

| Workload Type | Elements | Output Pages | Mean (ms) | P50 (ms) | P95 (ms) | P99 (ms) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Simple Shipping Label | 12 | 1 | 2.4 | 2.1 | 4.6 | 6.2 |
| Commercial Invoice | 45 | 1 | 4.3 | 4.1 | 7.2 | 8.7 |
| Bank Statement | 280 | 3 | 8.9 | 8.5 | 14.1 | 16.8 |
| Extended Audit Report | 750 | 8 | 17.5 | 16.8 | 26.2 | 31.0 |

### 2. Throughput Under Concurrent Load

Using Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) to serve concurrent requests:

| Concurrent Workers | Target Documents | Total Time (s) | Throughput (docs/sec) | Error Rate |
| :--- | :--- | :--- | :--- | :--- |
| 10 | 5,000 | 4.2 | 1,190 | 0.0% |
| 50 | 10,000 | 7.0 | 1,428 | 0.0% |
| 100 | 20,000 | 13.9 | 1,438 | 0.0% |
| 250 | 25,000 | 17.6 | 1,420 | 0.0% |

### 3. Memory Footprint Comparison

Comparison between PaperEngine and browser-based rendering approaches (generating 100 simultaneous documents):

| Engine | Process Architecture | Base RAM | Peak RAM Under 100 Concurrency | Startup Time |
| :--- | :--- | :--- | :--- | :--- |
| **PaperEngine** | In-Process JVM (Java 21) | 24 MB | 48 MB | 0.25 s |
| Headless Chrome | Subprocess Pool (Puppeteer) | 320 MB | 1,450 MB | 1.80 s |
| WeasyPrint | Python Subprocess | 85 MB | 420 MB | 1.10 s |

PaperEngine runs entirely in the host JVM process, avoiding foreign process coordination and out-of-memory crashes under load spikes.

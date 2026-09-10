package com.engine.paper.benchmark;

import com.engine.paper.domain.model.Document;
import com.engine.paper.renderer.PdfRenderer;
import com.engine.paper.template.TemplateCompiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class BenchmarkTest {

    @Test
    @DisplayName("Generates 500 documents across 100 Virtual Threads with 0% error rate")
    void concurrentVirtualThreadsRendering() throws InterruptedException {
        String template = """
                # Order #{{ orderId }}
                Customer: {{ customer }}
                [barcode:qr content="https://order.example/{{ orderId }}" size="60pt"]
                | Item | Amount |
                |---|---|
                | Cloud Cluster | $120.00 |
                """;

        String json = "{\"orderId\":\"ORD-8819\",\"customer\":\"Alexandr\"}";

        TemplateCompiler compiler = new TemplateCompiler();
        PdfRenderer renderer = new PdfRenderer();

        int docCount = 500;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long start = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(docCount);

            for (int i = 0; i < docCount; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        String payload = json.replace("ORD-8819", "ORD-" + id);
                        Document doc = compiler.compile(template, payload);
                        byte[] pdf = renderer.renderToBytes(doc);
                        if (pdf.length > 500) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        long elapsedNanos = System.nanoTime() - start;
        double elapsedSec = elapsedNanos / 1_000_000_000.0;
        double throughput = docCount / elapsedSec;

        assertThat(failureCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(docCount);
        assertThat(throughput).isGreaterThan(50.0); // Minimum throughput bar
    }
}

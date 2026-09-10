package com.engine.paper.infrastructure.cli;

import com.engine.paper.domain.model.Document;
import com.engine.paper.infrastructure.einvoice.PdfA3Packager;
import com.engine.paper.infrastructure.merger.DocumentMerger;
import com.engine.paper.infrastructure.scaffold.ScaffoldTemplates;
import com.engine.paper.infrastructure.server.PaperEngineServer;
import com.engine.paper.renderer.PdfRenderer;
import com.engine.paper.renderer.SvgRenderer;
import com.engine.paper.template.TemplateCompiler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Command line runner for PaperEngine rendering, HTTP serving, scaffolding, merging, and benchmarking.
 */
@Command(
        name = "paperengine",
        mixinStandardHelpOptions = true,
        version = "PaperEngine 1.0.0 (Java 21 LTS)",
        description = "Chromium-Free High-Speed PDF and Vector Report Engine",
        subcommands = {
                PaperEngineCli.RenderCommand.class,
                PaperEngineCli.ServeCommand.class,
                PaperEngineCli.InitCommand.class,
                PaperEngineCli.MergeCommand.class,
                PaperEngineCli.BenchmarkCommand.class
        }
)
public class PaperEngineCli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PaperEngineCli()).execute(args);
        System.exit(exitCode);
    }

    @Command(name = "render", description = "Render a Markdown/CSS template with JSON data to PDF or SVG")
    public static class RenderCommand implements Callable<Integer> {

        @Option(names = {"-t", "--template"}, required = true, description = "Path to Markdown template file")
        private File templateFile;

        @Option(names = {"-d", "--data"}, description = "Path to JSON dataset file (optional)")
        private File dataFile;

        @Option(names = {"-o", "--output"}, required = true, description = "Output destination file path")
        private File outputFile;

        @Option(names = {"-f", "--format"}, defaultValue = "pdf", description = "Target output format: pdf or svg (default: pdf)")
        private String format;

        @Option(names = {"--attach-xml"}, description = "Path to structured XML file to embed for Factur-X / PDF/A-3")
        private File attachXmlFile;

        @Option(names = {"--profile"}, defaultValue = "EN_16931", description = "Factur-X profile (e.g. EN_16931, BASIC, EXTENDED)")
        private String profile;

        @Override
        public Integer call() {
            try {
                if (!templateFile.exists()) {
                    System.err.println("Error: Template file not found: " + templateFile.getAbsolutePath());
                    return 1;
                }

                String template = Files.readString(templateFile.toPath());
                String data = dataFile != null && dataFile.exists()
                        ? Files.readString(dataFile.toPath())
                        : "{}";

                long start = System.nanoTime();
                TemplateCompiler compiler = new TemplateCompiler();
                Document document = compiler.compile(template, data);

                if (outputFile.getParentFile() != null) {
                    outputFile.getParentFile().mkdirs();
                }

                if ("svg".equalsIgnoreCase(format)) {
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        new SvgRenderer().render(document, fos);
                    }
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    new PdfRenderer().render(document, baos);
                    byte[] pdfBytes = baos.toByteArray();

                    if (attachXmlFile != null && attachXmlFile.exists()) {
                        byte[] xmlBytes = Files.readAllBytes(attachXmlFile.toPath());
                        PdfA3Packager.InvoiceProfile invProfile = PdfA3Packager.InvoiceProfile.fromString(profile);
                        pdfBytes = PdfA3Packager.packageFacturX(pdfBytes, xmlBytes, invProfile);
                    }

                    Files.write(outputFile.toPath(), pdfBytes);
                }

                double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
                System.out.printf("Successfully generated %s in %.2f ms (File: %s, %d bytes)%n",
                        format.toUpperCase(), elapsedMs, outputFile.getName(), outputFile.length());
                return 0;
            } catch (Exception e) {
                System.err.println("Rendering failed: " + e.getMessage());
                e.printStackTrace(System.err);
                return 1;
            }
        }
    }

    @Command(name = "init", description = "Scaffold a production-ready document template and mock JSON data")
    public static class InitCommand implements Callable<Integer> {

        @Parameters(index = "0", defaultValue = "invoice", description = "Template type: invoice, shipping-label, certificate, financial-report (default: invoice)")
        private String type;

        @Option(names = {"-d", "--dir"}, defaultValue = ".", description = "Target directory path for generated files (default: current directory)")
        private File targetDir;

        @Override
        public Integer call() {
            try {
                ScaffoldTemplates.scaffoldToDirectory(type, targetDir);
                ScaffoldTemplates.ScaffoldResult res = ScaffoldTemplates.get(type);
                System.out.println("Scaffolded template: " + new File(targetDir, res.templateFileName()).getPath());
                System.out.println("Scaffolded data file: " + new File(targetDir, res.dataFileName()).getPath());
                System.out.println("\nTo render your document, run:");
                System.out.printf("  paperengine render -t %s -d %s -o output.pdf%n",
                        new File(targetDir, res.templateFileName()).getPath(),
                        new File(targetDir, res.dataFileName()).getPath());
                return 0;
            } catch (Exception e) {
                System.err.println("Failed to scaffold template: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "merge", description = "Merge multiple PDF files into a single unified document")
    public static class MergeCommand implements Callable<Integer> {

        @Parameters(arity = "1..*", description = "Input PDF file paths to merge in order")
        private List<File> inputFiles;

        @Option(names = {"-o", "--output"}, required = true, description = "Merged output PDF file path")
        private File outputFile;

        @Override
        public Integer call() {
            try {
                long start = System.nanoTime();
                DocumentMerger.mergeFiles(inputFiles, outputFile);
                double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
                System.out.printf("Successfully merged %d files into %s in %.2f ms (%d bytes)%n",
                        inputFiles.size(), outputFile.getName(), elapsedMs, outputFile.length());
                return 0;
            } catch (Exception e) {
                System.err.println("Merge failed: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "serve", description = "Start the embedded virtual-threaded REST API and Web Studio server")
    public static class ServeCommand implements Callable<Integer> {

        @Option(names = {"-p", "--port"}, defaultValue = "8080", description = "HTTP port to bind (default: 8080)")
        private int port;

        @Override
        public Integer call() {
            try {
                PaperEngineServer server = new PaperEngineServer(port);
                server.start();

                System.out.printf("PaperEngine Studio is live at: http://localhost:%d/studio%n", port);
                System.out.println("Press Ctrl+C to stop server.");
                Thread.currentThread().join();
                return 0;
            } catch (Exception e) {
                System.err.println("Server execution error: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "benchmark", description = "Run concurrent throughput benchmarks on Virtual Threads")
    public static class BenchmarkCommand implements Callable<Integer> {

        @Option(names = {"-c", "--count"}, defaultValue = "1000", description = "Total documents to generate (default: 1000)")
        private int count;

        @Option(names = {"--concurrency"}, defaultValue = "50", description = "Virtual thread concurrency level (default: 50)")
        private int concurrency;

        @Override
        public Integer call() {
            System.out.printf("Starting benchmark: %d documents, %d concurrent workers (Java 21 Virtual Threads)%n",
                    count, concurrency);

            String template = """
                    # Invoice #{{ number }}
                    [barcode:qr content="https://pay.acme.example/{{ number }}" size="70pt"]
                    | Item | Qty | Price | Total |
                    |---|---|---|---|
                    | Widget Pro | 2 | $45.00 | $90.00 |
                    | Priority Delivery | 1 | $15.00 | $15.00 |
                    """;

            String json = "{\"number\":\"INV-2026-TEST\"}";
            TemplateCompiler compiler = new TemplateCompiler();
            PdfRenderer renderer = new PdfRenderer();

            // Warm up
            System.out.print("Warming up JIT... ");
            for (int i = 0; i < 50; i++) {
                try {
                    Document doc = compiler.compile(template, json);
                    renderer.renderToBytes(doc);
                } catch (Exception ignored) {
                }
            }
            System.out.println("Done.");

            List<Long> latenciesNanos = Collections.synchronizedList(new ArrayList<>(count));
            long totalStart = System.nanoTime();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CountDownLatch latch = new CountDownLatch(count);

                for (int i = 0; i < count; i++) {
                    executor.submit(() -> {
                        long opStart = System.nanoTime();
                        try {
                            Document doc = compiler.compile(template, json);
                            renderer.renderToBytes(doc);
                            latenciesNanos.add(System.nanoTime() - opStart);
                        } catch (Exception e) {
                            System.err.println("Benchmark error: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long totalElapsedNanos = System.nanoTime() - totalStart;
            double totalSec = totalElapsedNanos / 1_000_000_000.0;
            double throughput = count / totalSec;

            List<Long> sorted = new ArrayList<>(latenciesNanos);
            Collections.sort(sorted);

            double p50Ms = (sorted.get((int) (sorted.size() * 0.50))) / 1_000_000.0;
            double p95Ms = (sorted.get((int) (sorted.size() * 0.95))) / 1_000_000.0;
            double p99Ms = (sorted.get((int) (sorted.size() * 0.99))) / 1_000_000.0;

            Runtime rt = Runtime.getRuntime();
            long memoryMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

            System.out.println("=================================================");
            System.out.println("        PaperEngine Benchmark Results            ");
            System.out.println("=================================================");
            System.out.printf("Total Documents:     %d%n", count);
            System.out.printf("Total Time:          %.2f s%n", totalSec);
            System.out.printf("Throughput:          %.1f docs/sec%n", throughput);
            System.out.printf("P50 Latency:         %.2f ms%n", p50Ms);
            System.out.printf("P95 Latency:         %.2f ms%n", p95Ms);
            System.out.printf("P99 Latency:         %.2f ms%n", p99Ms);
            System.out.printf("Active Heap Memory:  %d MB%n", memoryMb);
            System.out.println("=================================================");

            return 0;
        }
    }
}

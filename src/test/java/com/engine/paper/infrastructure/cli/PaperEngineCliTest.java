package com.engine.paper.infrastructure.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class PaperEngineCliTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("CLI renders PDF from template and JSON data file")
    void cliRendersPdf() throws Exception {
        Path tmpl = tempDir.resolve("invoice.md");
        Files.writeString(tmpl, "# Test Invoice #{{ num }}\n\nTotal: $100.00\n");

        Path data = tempDir.resolve("data.json");
        Files.writeString(data, "{\"num\":\"INV-12345\"}");

        Path outPdf = tempDir.resolve("out.pdf");

        int exitCode = new CommandLine(new PaperEngineCli()).execute(
                "render",
                "--template", tmpl.toString(),
                "--data", data.toString(),
                "--output", outPdf.toString()
        );

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.exists(outPdf)).isTrue();
        assertThat(Files.size(outPdf)).isGreaterThan(100);
    }

    @Test
    @DisplayName("CLI renders SVG from template")
    void cliRendersSvg() throws Exception {
        Path tmpl = tempDir.resolve("vector.md");
        Files.writeString(tmpl, "# Scalable Report\n[barcode:qr content=\"https://example.com\" size=\"50pt\"]\n");

        Path outSvg = tempDir.resolve("out.svg");

        int exitCode = new CommandLine(new PaperEngineCli()).execute(
                "render",
                "--template", tmpl.toString(),
                "--format", "svg",
                "--output", outSvg.toString()
        );

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.exists(outSvg)).isTrue();
        String content = Files.readString(outSvg);
        assertThat(content).contains("<svg");
        assertThat(content).contains("Scalable Report");
    }

    @Test
    @DisplayName("CLI --help displays usage and returns 0")
    void cliDisplaysHelp() {
        int exitCode = new CommandLine(new PaperEngineCli()).execute("--help");
        assertThat(exitCode).isEqualTo(0);
    }
}

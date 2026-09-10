package com.engine.paper.infrastructure.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class PaperEngineServerTest {

    private static final int PORT = 18080;
    private static PaperEngineServer server;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws IOException {
        server = new PaperEngineServer(PORT);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @DisplayName("GET /health returns 200 OK with UP status")
    void healthCheck() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        assertThat(response.body()).contains("Virtual Threads");
    }

    @Test
    @DisplayName("GET /metrics returns 200 OK with memory stats")
    void metricsCheck() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/metrics"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("usedMemoryMb");
    }

    @Test
    @DisplayName("POST /api/v1/render returns 200 OK with binary PDF stream")
    void renderPdfOverHttp() throws Exception {
        String payload = """
                {
                  "template": "# Invoice #{{ invNum }}\\n\\nTotal Due: $500.00\\n",
                  "data": { "invNum": "INV-2026-HTTP" },
                  "format": "pdf"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/v1/render"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/pdf");
        assertThat(new String(response.body(), 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("POST /api/v1/render returns 200 OK with SVG stream when format is svg")
    void renderSvgOverHttp() throws Exception {
        String payload = """
                {
                  "template": "# Vector SVG Document\\nClean scalable output.\\n",
                  "data": {},
                  "format": "svg"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/v1/render"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("image/svg+xml");
        assertThat(response.body()).contains("<svg");
        assertThat(response.body()).contains("Vector SVG Document");
    }

    @Test
    @DisplayName("GET /studio returns 200 OK with PaperEngine Web Studio HTML")
    void studioEndpointCheck() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/studio"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/html");
        assertThat(response.body()).contains("PaperEngine Studio");
        assertThat(response.body()).contains("code-editor");
    }

    @Test
    @DisplayName("GET / root returns 200 OK serving Web Studio")
    void rootEndpointCheck() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/html");
        assertThat(response.body()).contains("PaperEngine Studio");
    }
}

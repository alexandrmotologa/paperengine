package com.engine.paper.infrastructure.server;

import com.engine.paper.domain.model.Document;
import com.engine.paper.renderer.PdfRenderer;
import com.engine.paper.renderer.SvgRenderer;
import com.engine.paper.template.TemplateCompiler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * High-performance, lightweight REST API server powered by Java 21 Virtual Threads.
 * Requires zero heavyweight servlet containers and serves documents with sub-millisecond dispatch.
 */
public class PaperEngineServer {

    private final int port;
    private HttpServer server;
    private final TemplateCompiler compiler = new TemplateCompiler();
    private final PdfRenderer pdfRenderer = new PdfRenderer();
    private final SvgRenderer svgRenderer = new SvgRenderer();
    private final ObjectMapper mapper = new ObjectMapper();

    public PaperEngineServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // Bind to Java 21 Virtual Threads executor
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/api/v1/render", this::handleRender);
        server.createContext("/health", this::handleHealth);
        server.createContext("/metrics", this::handleMetrics);

        server.start();
        System.out.println("PaperEngine server started on port " + port + " (Virtual Threads enabled)");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleRender(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            JsonNode body = mapper.readTree(is);
            if (body == null || !body.has("template")) {
                sendResponse(exchange, 400, "{\"error\":\"Missing 'template' property in JSON payload\"}", "application/json");
                return;
            }

            String template = body.get("template").asText();
            String dataJson = body.has("data") ? body.get("data").toString() : "{}";
            String format = body.has("format") ? body.get("format").asText("pdf").toLowerCase() : "pdf";

            Document document = compiler.compile(template, dataJson);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if ("svg".equalsIgnoreCase(format)) {
                svgRenderer.render(document, baos);
                byte[] bytes = baos.toByteArray();
                exchange.getResponseHeaders().set("Content-Type", "image/svg+xml");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                pdfRenderer.render(document, baos);
                byte[] bytes = baos.toByteArray();
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                exchange.getResponseHeaders().set("Content-Disposition", "inline; filename=\"document.pdf\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        } catch (Exception e) {
            String err = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
            sendResponse(exchange, 500, err, "application/json");
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        String json = "{\"status\":\"UP\",\"engine\":\"PaperEngine\",\"runtime\":\"Java 21 Virtual Threads\"}";
        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        Runtime rt = Runtime.getRuntime();
        long usedMemMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMemMb = rt.maxMemory() / (1024 * 1024);

        String json = String.format("{\"usedMemoryMb\":%d,\"maxMemoryMb\":%d,\"availableProcessors\":%d}",
                usedMemMb, maxMemMb, rt.availableProcessors());
        sendResponse(exchange, 200, json, "application/json");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

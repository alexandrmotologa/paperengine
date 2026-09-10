package com.engine.paper.domain.style;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry managing custom TrueType (.ttf) and OpenType (.otf) font definitions.
 * Pure domain model with zero dependency on PDF rendering libraries.
 */
public class FontRegistry {

    private static final FontRegistry DEFAULT_INSTANCE = new FontRegistry();

    public static FontRegistry getDefault() {
        return DEFAULT_INSTANCE;
    }

    public record FontDefinition(
            String family,
            boolean bold,
            boolean italic,
            String sourcePath,
            byte[] fontData
    ) {
    }

    private final Map<String, FontDefinition> registeredFonts = new ConcurrentHashMap<>();

    public FontRegistry() {
    }

    public void registerFont(String family, String filePath) throws IOException {
        registerFont(family, false, false, filePath);
    }

    public void registerFont(String family, boolean bold, boolean italic, String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Font file path cannot be blank");
        }
        File file = new File(filePath);
        byte[] data = null;
        if (file.exists() && file.isFile()) {
            data = Files.readAllBytes(file.toPath());
        }
        registerFont(family, bold, italic, filePath, data);
    }

    public void registerFont(String family, byte[] data) {
        registerFont(family, false, false, null, data);
    }

    public void registerFont(String family, boolean bold, boolean italic, byte[] data) {
        registerFont(family, bold, italic, null, data);
    }

    public void registerFont(String family, boolean bold, boolean italic, String sourcePath, byte[] data) {
        if (family == null || family.isBlank()) {
            return;
        }
        String key = fontKey(family, bold, italic);
        registeredFonts.put(key, new FontDefinition(family, bold, italic, sourcePath, data));
    }

    public FontDefinition getFont(String family, boolean bold, boolean italic) {
        if (family == null || family.isBlank()) {
            return null;
        }
        String specificKey = fontKey(family, bold, italic);
        FontDefinition def = registeredFonts.get(specificKey);
        if (def != null) {
            return def;
        }
        // Fallback to regular variant of the same family
        return registeredFonts.get(fontKey(family, false, false));
    }

    public boolean hasFont(String family) {
        if (family == null) return false;
        String normalized = family.trim().toLowerCase();
        return registeredFonts.keySet().stream().anyMatch(k -> k.startsWith(normalized + ":"));
    }

    public void clear() {
        registeredFonts.clear();
    }

    private String fontKey(String family, boolean bold, boolean italic) {
        return family.trim().toLowerCase() + ":" + (bold ? "b" : "r") + ":" + (italic ? "i" : "r");
    }
}

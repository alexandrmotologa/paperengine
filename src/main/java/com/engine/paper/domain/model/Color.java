package com.engine.paper.domain.model;

/**
 * RGBA color model with hex parser and normalization.
 */
public record Color(int red, int green, int blue, double alpha) {

    public static final Color BLACK = new Color(0, 0, 0, 1.0);
    public static final Color WHITE = new Color(255, 255, 255, 1.0);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0.0);
    public static final Color SLATE_900 = new Color(15, 23, 42, 1.0);
    public static final Color SLATE_800 = new Color(30, 41, 59, 1.0);
    public static final Color SLATE_700 = new Color(51, 65, 85, 1.0);
    public static final Color SLATE_200 = new Color(226, 232, 240, 1.0);
    public static final Color SLATE_100 = new Color(241, 245, 249, 1.0);
    public static final Color CYAN_ACCENT = new Color(0, 245, 255, 1.0);

    public Color(int red, int green, int blue) {
        this(red, green, blue, 1.0);
    }

    public static Color rgb(int r, int g, int b) {
        return new Color(r, g, b, 1.0);
    }

    public static Color rgba(int r, int g, int b, double a) {
        return new Color(r, g, b, a);
    }

    public static Color parse(String str) {
        if (str == null || str.isBlank()) {
            return BLACK;
        }
        String s = str.trim();
        if (s.startsWith("#") || (!s.startsWith("rgb") && !s.contains("("))) {
            return parseHex(s);
        }
        if (s.toLowerCase().startsWith("rgba") || s.toLowerCase().startsWith("rgb")) {
            try {
                int open = s.indexOf('(');
                int close = s.indexOf(')');
                if (open > 0 && close > open) {
                    String[] parts = s.substring(open + 1, close).split("[,/]");
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    double a = parts.length > 3 ? Double.parseDouble(parts[3].trim()) : 1.0;
                    return new Color(r, g, b, a);
                }
            } catch (Exception ignored) {
            }
        }
        return parseHex(s);
    }

    public static Color parseHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return BLACK;
        }
        String clean = hex.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if ("transparent".equalsIgnoreCase(clean)) {
            return TRANSPARENT;
        }

        try {
            if (clean.length() == 3) {
                int r = Integer.parseInt(clean.substring(0, 1) + clean.substring(0, 1), 16);
                int g = Integer.parseInt(clean.substring(1, 2) + clean.substring(1, 2), 16);
                int b = Integer.parseInt(clean.substring(2, 3) + clean.substring(2, 3), 16);
                return new Color(r, g, b, 1.0);
            } else if (clean.length() == 6) {
                int r = Integer.parseInt(clean.substring(0, 2), 16);
                int g = Integer.parseInt(clean.substring(2, 4), 16);
                int b = Integer.parseInt(clean.substring(4, 6), 16);
                return new Color(r, g, b, 1.0);
            } else if (clean.length() == 8) {
                int r = Integer.parseInt(clean.substring(0, 2), 16);
                int g = Integer.parseInt(clean.substring(2, 4), 16);
                int b = Integer.parseInt(clean.substring(4, 6), 16);
                int a = Integer.parseInt(clean.substring(6, 8), 16);
                return new Color(r, g, b, a / 255.0);
            }
        } catch (NumberFormatException ignored) {
            // fallback to black on invalid hex
        }
        return BLACK;
    }

    public float rFloat() {
        return red / 255.0f;
    }

    public float gFloat() {
        return green / 255.0f;
    }

    public float bFloat() {
        return blue / 255.0f;
    }

    public String toSvgHex() {
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}

package com.engine.paper.domain.style;

/**
 * Strategy interface for measuring text string widths and heights.
 * Keeps the domain model decoupled from low-level font rendering libraries.
 */
public interface FontMetricsProvider {

    double measureTextWidth(String text, TextStyle style);

    double getLineHeight(TextStyle style);

    /**
     * Default metric estimation based on standard typography proportions
     * (average glyph width ~0.52 * font size for standard proportional sans-serif).
     */
    FontMetricsProvider DEFAULT = new FontMetricsProvider() {
        @Override
        public double measureTextWidth(String text, TextStyle style) {
            if (text == null || text.isEmpty()) {
                return 0.0;
            }
            double avgWidthRatio = style.fontWeight() == TextStyle.FontWeight.BOLD ? 0.56 : 0.50;
            return text.length() * style.fontSize() * avgWidthRatio;
        }

        @Override
        public double getLineHeight(TextStyle style) {
            return style.calculateLineHeight();
        }
    };
}

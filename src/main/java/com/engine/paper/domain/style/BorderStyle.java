package com.engine.paper.domain.style;

import com.engine.paper.domain.model.Color;

/**
 * Geometric borders and corner rounding applied to elements.
 */
public record BorderStyle(
        double topWidth,
        double rightWidth,
        double bottomWidth,
        double leftWidth,
        Color color,
        double borderRadius
) {
    public static final BorderStyle NONE = new BorderStyle(0, 0, 0, 0, Color.TRANSPARENT, 0);

    public static BorderStyle solid(double width, Color color) {
        return new BorderStyle(width, width, width, width, color, 0);
    }

    public static BorderStyle solidRounded(double width, Color color, double radius) {
        return new BorderStyle(width, width, width, width, color, radius);
    }

    public static BorderStyle bottom(double width, Color color) {
        return new BorderStyle(0, 0, width, 0, color, 0);
    }

    public boolean hasBorder() {
        return (topWidth > 0 || rightWidth > 0 || bottomWidth > 0 || leftWidth > 0) && color.alpha() > 0;
    }
}

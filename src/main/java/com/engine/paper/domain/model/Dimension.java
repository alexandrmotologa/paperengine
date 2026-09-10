package com.engine.paper.domain.model;

/**
 * 2D size measurements in points.
 */
public record Dimension(double width, double height) {
    public static final Dimension ZERO = new Dimension(0, 0);

    public static final double AUTO = -1.0;

    public boolean isWidthAuto() {
        return width < 0;
    }

    public boolean isHeightAuto() {
        return height < 0;
    }
}

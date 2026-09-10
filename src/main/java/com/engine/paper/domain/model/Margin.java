package com.engine.paper.domain.model;

/**
 * Exterior margins for a document page or container element in points.
 */
public record Margin(double top, double right, double bottom, double left) {

    public static final Margin ZERO = new Margin(0, 0, 0, 0);

    public static Margin all(double value) {
        return new Margin(value, value, value, value);
    }

    public static Margin symmetric(double vertical, double horizontal) {
        return new Margin(vertical, horizontal, vertical, horizontal);
    }

    public double horizontalTotal() {
        return left + right;
    }

    public double verticalTotal() {
        return top + bottom;
    }
}

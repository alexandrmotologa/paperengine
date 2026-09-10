package com.engine.paper.domain.model;

/**
 * Interior padding for container elements in points.
 */
public record Padding(double top, double right, double bottom, double left) {

    public static final Padding ZERO = new Padding(0, 0, 0, 0);

    public static Padding all(double value) {
        return new Padding(value, value, value, value);
    }

    public static Padding symmetric(double vertical, double horizontal) {
        return new Padding(vertical, horizontal, vertical, horizontal);
    }

    public double horizontalTotal() {
        return left + right;
    }

    public double verticalTotal() {
        return top + bottom;
    }
}

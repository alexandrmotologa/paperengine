package com.engine.paper.domain.model;

/**
 * 2D coordinate in desktop publishing points.
 */
public record Point(double x, double y) {
    public static final Point ZERO = new Point(0, 0);

    public Point translate(double dx, double dy) {
        return new Point(x + dx, y + dy);
    }
}

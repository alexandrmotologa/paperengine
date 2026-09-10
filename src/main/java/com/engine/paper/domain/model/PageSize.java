package com.engine.paper.domain.model;

/**
 * Standard and custom page dimensions in desktop publishing points (1/72 inch).
 */
public record PageSize(String name, double width, double height) {

    public static final PageSize A3 = new PageSize("A3", 841.89, 1190.55);
    public static final PageSize A4 = new PageSize("A4", 595.28, 841.89);
    public static final PageSize A5 = new PageSize("A5", 419.53, 595.28);
    public static final PageSize LETTER = new PageSize("LETTER", 612.0, 792.0);
    public static final PageSize LEGAL = new PageSize("LEGAL", 612.0, 1008.0);

    public static PageSize custom(double width, double height) {
        return new PageSize("CUSTOM", width, height);
    }

    public PageSize toLandscape() {
        if (width > height) {
            return this;
        }
        return new PageSize(name + "_LANDSCAPE", height, width);
    }

    public PageSize toPortrait() {
        if (height > width) {
            return this;
        }
        return new PageSize(name + "_PORTRAIT", height, width);
    }
}

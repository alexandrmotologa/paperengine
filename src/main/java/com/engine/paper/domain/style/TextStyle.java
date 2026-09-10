package com.engine.paper.domain.style;

import com.engine.paper.domain.model.Color;

/**
 * Typography styles applied to text blocks.
 */
public record TextStyle(
        String fontFamily,
        double fontSize,
        FontWeight fontWeight,
        FontStyle fontStyle,
        double lineHeightMultiplier,
        Color color,
        TextAlign textAlign,
        double letterSpacing
) {
    public static final TextStyle DEFAULT = new TextStyle(
            "Helvetica",
            10.0,
            FontWeight.NORMAL,
            FontStyle.NORMAL,
            1.35,
            Color.SLATE_900,
            TextAlign.LEFT,
            0.0
    );

    public static final TextStyle H1 = new TextStyle(
            "Helvetica",
            22.0,
            FontWeight.BOLD,
            FontStyle.NORMAL,
            1.2,
            Color.SLATE_900,
            TextAlign.LEFT,
            -0.2
    );

    public static final TextStyle H2 = new TextStyle(
            "Helvetica",
            16.0,
            FontWeight.BOLD,
            FontStyle.NORMAL,
            1.25,
            Color.SLATE_900,
            TextAlign.LEFT,
            0.0
    );

    public static final TextStyle H3 = new TextStyle(
            "Helvetica",
            13.0,
            FontWeight.BOLD,
            FontStyle.NORMAL,
            1.3,
            Color.SLATE_800,
            TextAlign.LEFT,
            0.0
    );

    public enum FontWeight {
        NORMAL,
        BOLD
    }

    public enum FontStyle {
        NORMAL,
        ITALIC
    }

    public enum TextAlign {
        LEFT,
        CENTER,
        RIGHT,
        JUSTIFY
    }

    public double calculateLineHeight() {
        return fontSize * lineHeightMultiplier;
    }

    public TextStyle withFontSize(double size) {
        return new TextStyle(fontFamily, size, fontWeight, fontStyle, lineHeightMultiplier, color, textAlign, letterSpacing);
    }

    public TextStyle withColor(Color newColor) {
        return new TextStyle(fontFamily, fontSize, fontWeight, fontStyle, lineHeightMultiplier, newColor, textAlign, letterSpacing);
    }

    public TextStyle withBold(boolean bold) {
        return new TextStyle(fontFamily, fontSize, bold ? FontWeight.BOLD : FontWeight.NORMAL, fontStyle, lineHeightMultiplier, color, textAlign, letterSpacing);
    }

    public TextStyle withTextAlign(TextAlign align) {
        return new TextStyle(fontFamily, fontSize, fontWeight, fontStyle, lineHeightMultiplier, color, align, letterSpacing);
    }
}

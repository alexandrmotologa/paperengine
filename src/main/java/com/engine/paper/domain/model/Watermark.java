package com.engine.paper.domain.model;

/**
 * Domain model representing a printable watermark or stamp overlay across document pages.
 */
public class Watermark {

    private String text = "CONFIDENTIAL";
    private Color color = new Color(220, 38, 38, 0.15); // Subtle red by default
    private double angle = 45.0; // 45 degrees diagonal
    private double fontSize = 64.0;
    private String fontFamily = "Helvetica";

    public Watermark() {
    }

    public Watermark(String text) {
        this.text = text != null ? text : "";
    }

    public Watermark(String text, Color color, double angle, double fontSize) {
        this.text = text != null ? text : "";
        this.color = color != null ? color : new Color(220, 38, 38, 0.15);
        this.angle = angle;
        this.fontSize = fontSize > 0 ? fontSize : 64.0;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color != null ? color : new Color(220, 38, 38, 0.15);
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize > 0 ? fontSize : 64.0;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily != null ? fontFamily : "Helvetica";
    }

    public boolean isVisible() {
        return text != null && !text.isBlank() && color != null && color.alpha() > 0;
    }
}

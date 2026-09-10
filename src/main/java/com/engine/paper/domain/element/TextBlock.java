package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;
import com.engine.paper.domain.style.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Text element with typography styles and wrapped line segments.
 */
public class TextBlock implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();
    private String text;
    private int headingLevel = 0; // 0 = normal text, 1 = H1, 2 = H2, etc.
    private final List<String> lines = new ArrayList<>();

    public TextBlock(String text) {
        this.text = text != null ? text : "";
    }

    public TextBlock(String text, TextStyle textStyle) {
        this.text = text != null ? text : "";
        this.style.setTextStyle(textStyle);
    }

    public static TextBlock h1(String text) {
        TextBlock block = new TextBlock(text, TextStyle.H1);
        block.headingLevel = 1;
        return block;
    }

    public static TextBlock h2(String text) {
        TextBlock block = new TextBlock(text, TextStyle.H2);
        block.headingLevel = 2;
        return block;
    }

    public static TextBlock h3(String text) {
        TextBlock block = new TextBlock(text, TextStyle.H3);
        block.headingLevel = 3;
        return block;
    }

    @Override
    public ElementType getType() {
        return ElementType.TEXT;
    }

    @Override
    public BoxDimensions getDimensions() {
        return dimensions;
    }

    @Override
    public ElementStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(ElementStyle style) {
        this.style = style != null ? style : new ElementStyle();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public int getHeadingLevel() {
        return headingLevel;
    }

    public void setHeadingLevel(int headingLevel) {
        this.headingLevel = headingLevel;
    }

    public boolean isHeading() {
        return headingLevel >= 1;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> newLines) {
        this.lines.clear();
        if (newLines != null) {
            this.lines.addAll(newLines);
        }
    }

    public TextStyle getTextStyle() {
        return style.getTextStyle();
    }
}

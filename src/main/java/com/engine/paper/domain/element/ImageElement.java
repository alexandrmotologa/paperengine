package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;

/**
 * Bitmap or vector image element embedded in the document.
 */
public class ImageElement implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();
    private byte[] rawData;
    private String mimeType = "image/png";
    private String altText = "";

    public ImageElement(byte[] rawData, double width, double height, String mimeType) {
        this.rawData = rawData;
        this.style.setWidth(width);
        this.style.setHeight(height);
        this.mimeType = mimeType != null ? mimeType : "image/png";
    }

    @Override
    public ElementType getType() {
        return ElementType.IMAGE;
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

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText != null ? altText : "";
    }
}

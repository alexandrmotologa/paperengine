package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Container element that groups children and applies flexbox layout rules.
 */
public class ContainerElement implements DocumentElement {

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style;
    private final List<DocumentElement> children = new ArrayList<>();

    public ContainerElement() {
        this.style = new ElementStyle();
    }

    public ContainerElement(ElementStyle style) {
        this.style = style != null ? style : new ElementStyle();
    }

    @Override
    public ElementType getType() {
        return ElementType.CONTAINER;
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

    @Override
    public List<DocumentElement> getChildren() {
        return children;
    }

    public ContainerElement addChild(DocumentElement child) {
        if (child != null) {
            this.children.add(child);
        }
        return this;
    }

    public ContainerElement addChildren(List<? extends DocumentElement> elements) {
        if (elements != null) {
            this.children.addAll(elements);
        }
        return this;
    }

    @Override
    public boolean isSplittable() {
        return true;
    }
}

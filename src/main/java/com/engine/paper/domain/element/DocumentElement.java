package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.ElementStyle;

import java.util.Collections;
import java.util.List;

/**
 * Base interface for all layout tree elements.
 */
public interface DocumentElement {

    ElementType getType();

    BoxDimensions getDimensions();

    ElementStyle getStyle();

    void setStyle(ElementStyle style);

    default List<DocumentElement> getChildren() {
        return Collections.emptyList();
    }

    default boolean isSplittable() {
        return false;
    }
}

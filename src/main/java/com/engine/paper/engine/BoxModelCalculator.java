package com.engine.paper.engine;

import com.engine.paper.domain.element.DocumentElement;
import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.style.BorderStyle;
import com.engine.paper.domain.style.ElementStyle;

/**
 * Calculates box-model boundaries for layout elements.
 */
public class BoxModelCalculator {

    /**
     * Initializes the padding, border, and margin properties on an element's BoxDimensions.
     */
    public static void initializeDimensions(DocumentElement element) {
        if (element == null) {
            return;
        }

        BoxDimensions dimensions = element.getDimensions();
        ElementStyle style = element.getStyle();

        dimensions.setMargin(style.getMargin());
        dimensions.setPadding(style.getPadding());

        BorderStyle border = style.getBorderStyle();
        if (border != null) {
            dimensions.setBorderWidths(
                    border.topWidth(),
                    border.rightWidth(),
                    border.bottomWidth(),
                    border.leftWidth()
            );
        } else {
            dimensions.setBorderWidths(0, 0, 0, 0);
        }
    }

    /**
     * Resolves the target content width given available container width and element styles.
     */
    public static double resolveContentWidth(DocumentElement element, double availableWidth) {
        ElementStyle style = element.getStyle();
        BoxDimensions dimensions = element.getDimensions();

        double borderPaddingHorizontal = dimensions.getPadding().horizontalTotal()
                + dimensions.getBorderLeft() + dimensions.getBorderRight();
        double marginHorizontal = dimensions.getMargin().horizontalTotal();

        if (style.getWidth() >= 0) {
            return Math.max(0, style.getWidth() - borderPaddingHorizontal);
        }

        double width = availableWidth - marginHorizontal - borderPaddingHorizontal;
        return Math.max(0, width);
    }
}

package com.engine.paper.engine;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.layout.*;
import com.engine.paper.domain.style.ElementStyle;
import com.engine.paper.domain.style.FontMetricsProvider;
import com.engine.paper.domain.style.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * High-speed vector layout solver implementing CSS-style flexbox and box models.
 */
public class FlexLayoutSolver {

    private final FontMetricsProvider fontMetrics;

    public FlexLayoutSolver() {
        this(FontMetricsProvider.DEFAULT);
    }

    public FlexLayoutSolver(FontMetricsProvider fontMetrics) {
        this.fontMetrics = fontMetrics != null ? fontMetrics : FontMetricsProvider.DEFAULT;
    }

    /**
     * Solves layout for a root element, determining exact coordinates and dimensions.
     */
    public void solve(DocumentElement element, double x, double y, double availableWidth) {
        if (element == null) {
            return;
        }

        BoxModelCalculator.initializeDimensions(element);
        BoxDimensions dims = element.getDimensions();
        dims.setX(x);
        dims.setY(y);

        double contentWidth = BoxModelCalculator.resolveContentWidth(element, availableWidth);
        dims.setContentWidth(contentWidth);

        if (element instanceof TextBlock textBlock) {
            solveTextBlock(textBlock, contentWidth);
        } else if (element instanceof TableElement table) {
            solveTable(table, contentWidth);
        } else if (element instanceof BarcodeElement barcode) {
            solveBarcode(barcode);
        } else if (element instanceof ImageElement image) {
            solveImage(image);
        } else if (element instanceof DividerElement divider) {
            solveDivider(divider, contentWidth);
        } else if (element instanceof ContainerElement container) {
            solveContainer(container, contentWidth);
        }
    }

    private void solveTextBlock(TextBlock textBlock, double availableWidth) {
        BoxDimensions dims = textBlock.getDimensions();
        ElementStyle style = textBlock.getStyle();
        TextStyle textStyle = style.getTextStyle();

        String raw = textBlock.getText();
        List<String> wrappedLines = wrapText(raw, textStyle, availableWidth);
        textBlock.setLines(wrappedLines);

        double lineHeight = fontMetrics.getLineHeight(textStyle);
        double naturalHeight = Math.max(lineHeight, wrappedLines.size() * lineHeight);
        double contentHeight = style.getHeight() >= 0 ? style.getHeight() : naturalHeight;
        dims.setContentHeight(contentHeight);
    }

    private List<String> wrapText(String text, TextStyle textStyle, double maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] paragraphs = text.split("\r?\n");
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (currentLine.isEmpty()) {
                    currentLine.append(word);
                } else {
                    String testLine = currentLine + " " + word;
                    double width = fontMetrics.measureTextWidth(testLine, textStyle);
                    if (width <= maxWidth) {
                        currentLine.append(" ").append(word);
                    } else {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    }
                }
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
        }
        return lines;
    }

    private void solveBarcode(BarcodeElement barcode) {
        BoxDimensions dims = barcode.getDimensions();
        ElementStyle style = barcode.getStyle();

        double w = style.getWidth() > 0 ? style.getWidth() : 80.0;
        double h = style.getHeight() > 0 ? style.getHeight() : 80.0;

        dims.setContentWidth(w);
        dims.setContentHeight(h);
    }

    private void solveImage(ImageElement image) {
        BoxDimensions dims = image.getDimensions();
        ElementStyle style = image.getStyle();

        double w = style.getWidth() > 0 ? style.getWidth() : 120.0;
        double h = style.getHeight() > 0 ? style.getHeight() : 80.0;

        dims.setContentWidth(w);
        dims.setContentHeight(h);
    }

    private void solveDivider(DividerElement divider, double availableWidth) {
        BoxDimensions dims = divider.getDimensions();
        ElementStyle style = divider.getStyle();

        dims.setContentWidth(availableWidth);
        dims.setContentHeight(style.getHeight() > 0 ? style.getHeight() : 1.0);
    }

    private void solveTable(TableElement table, double availableWidth) {
        BoxDimensions dims = table.getDimensions();
        List<Double> colWeights = table.getColumnWidths();

        // Determine column count
        int colCount = colWeights.size();
        if (colCount == 0) {
            if (!table.getHeaderRows().isEmpty()) {
                colCount = table.getHeaderRows().getFirst().getCells().size();
            } else if (!table.getBodyRows().isEmpty()) {
                colCount = table.getBodyRows().getFirst().getCells().size();
            }
            colCount = Math.max(1, colCount);
        }

        // Calculate resolved column widths in points
        List<Double> resolvedColWidths = new ArrayList<>(colCount);
        if (!colWeights.isEmpty() && colWeights.size() == colCount) {
            double totalWeight = 0;
            for (double w : colWeights) {
                totalWeight += w;
            }
            for (double w : colWeights) {
                resolvedColWidths.add((w / totalWeight) * availableWidth);
            }
        } else {
            double equalWidth = availableWidth / colCount;
            for (int i = 0; i < colCount; i++) {
                resolvedColWidths.add(equalWidth);
            }
        }

        double cursorY = dims.getContentBoxY();
        double totalHeight = 0;

        // Solve header rows
        for (TableRow row : table.getHeaderRows()) {
            double rowHeight = solveTableRow(row, dims.getContentBoxX(), cursorY, resolvedColWidths);
            cursorY += rowHeight;
            totalHeight += rowHeight;
        }

        // Solve body rows
        for (TableRow row : table.getBodyRows()) {
            double rowHeight = solveTableRow(row, dims.getContentBoxX(), cursorY, resolvedColWidths);
            cursorY += rowHeight;
            totalHeight += rowHeight;
        }

        dims.setContentHeight(totalHeight);
    }

    private double solveTableRow(TableRow row, double startX, double startY, List<Double> colWidths) {
        BoxModelCalculator.initializeDimensions(row);
        BoxDimensions rowDims = row.getDimensions();
        rowDims.setX(startX);
        rowDims.setY(startY);

        double totalWidth = 0;
        for (double w : colWidths) {
            totalWidth += w;
        }
        rowDims.setContentWidth(totalWidth);

        double currentCellX = startX;
        double maxCellHeight = 0;

        List<TableCell> cells = row.getCells();
        int colIdx = 0;

        for (TableCell cell : cells) {
            if (colIdx >= colWidths.size()) {
                break;
            }

            int span = Math.min(cell.getColSpan(), colWidths.size() - colIdx);
            double cellWidth = 0;
            for (int s = 0; s < span; s++) {
                cellWidth += colWidths.get(colIdx + s);
            }

            BoxModelCalculator.initializeDimensions(cell);
            BoxDimensions cellDims = cell.getDimensions();
            cellDims.setX(currentCellX);
            cellDims.setY(startY);
            cellDims.setContentWidth(cellWidth - cellDims.getPadding().horizontalTotal()
                    - cellDims.getBorderLeft() - cellDims.getBorderRight());

            if (cell.getContent() != null) {
                solve(cell.getContent(), cellDims.getContentBoxX(), cellDims.getContentBoxY(), cellDims.getContentWidth());
                double innerHeight = cell.getContent().getDimensions().getTotalHeight();
                cellDims.setContentHeight(innerHeight);
            }

            double cellTotalH = cellDims.getBorderBoxHeight();
            if (cellTotalH > maxCellHeight) {
                maxCellHeight = cellTotalH;
            }

            currentCellX += cellWidth;
            colIdx += span;
        }

        double finalRowHeight = Math.max(row.getStyle().getHeight(), maxCellHeight);
        rowDims.setContentHeight(finalRowHeight);

        // Normalize cell heights in row for vertical stretch
        for (TableCell cell : cells) {
            cell.getDimensions().setContentHeight(
                    finalRowHeight - cell.getDimensions().getPadding().verticalTotal()
                            - cell.getDimensions().getBorderTop() - cell.getDimensions().getBorderBottom()
            );
        }

        return rowDims.getTotalHeight();
    }

    private void solveContainer(ContainerElement container, double availableWidth) {
        BoxDimensions dims = container.getDimensions();
        ElementStyle style = container.getStyle();

        if (style.getDisplay() == ElementStyle.Display.FLEX) {
            if (style.getFlexDirection().isRow()) {
                solveFlexRow(container, availableWidth);
            } else {
                solveFlexColumn(container, availableWidth);
            }
        } else {
            // Block display acts as a vertical column flow
            solveFlexColumn(container, availableWidth);
        }
    }

    private void solveFlexRow(ContainerElement container, double availableWidth) {
        BoxDimensions dims = container.getDimensions();
        ElementStyle style = container.getStyle();
        List<DocumentElement> children = container.getChildren();

        if (children.isEmpty()) {
            dims.setContentHeight(0);
            return;
        }

        double startX = dims.getContentBoxX();
        double startY = dims.getContentBoxY();
        double columnGap = style.getColumnGap();

        // 1st Pass: Initial measurement of child elements
        double totalNaturalWidth = 0;
        double totalFlexGrow = 0;

        for (DocumentElement child : children) {
            BoxModelCalculator.initializeDimensions(child);
            ElementStyle childStyle = child.getStyle();
            totalFlexGrow += childStyle.getFlexGrow();

            double childWidth = childStyle.getWidth() >= 0
                    ? childStyle.getWidth()
                    : (availableWidth / children.size());

            solve(child, startX, startY, childWidth);
            totalNaturalWidth += child.getDimensions().getTotalWidth();
        }

        double totalGaps = columnGap * (children.size() - 1);
        double extraSpace = availableWidth - totalNaturalWidth - totalGaps;

        // Distribute flex-grow if positive extra space
        if (extraSpace > 0 && totalFlexGrow > 0) {
            totalNaturalWidth = 0;
            for (DocumentElement child : children) {
                ElementStyle childStyle = child.getStyle();
                if (childStyle.getFlexGrow() > 0) {
                    double addedWidth = extraSpace * (childStyle.getFlexGrow() / totalFlexGrow);
                    double newWidth = child.getDimensions().getBorderBoxWidth() + addedWidth;
                    solve(child, startX, startY, newWidth);
                }
                totalNaturalWidth += child.getDimensions().getTotalWidth();
            }
            extraSpace = Math.max(0, availableWidth - totalNaturalWidth - totalGaps);
        }

        // Justify-content offsets
        double currentX = startX;
        double spacing = 0;

        switch (style.getJustifyContent()) {
            case CENTER -> currentX += extraSpace / 2.0;
            case FLEX_END -> currentX += extraSpace;
            case SPACE_BETWEEN -> {
                if (children.size() > 1) {
                    spacing = extraSpace / (children.size() - 1);
                }
            }
            case SPACE_AROUND -> {
                double unit = extraSpace / (children.size() * 2.0);
                currentX += unit;
                spacing = unit * 2.0;
            }
            case SPACE_EVENLY -> {
                double unit = extraSpace / (children.size() + 1.0);
                currentX += unit;
                spacing = unit;
            }
            case FLEX_START -> {
                // currentX remains startX
            }
        }

        double maxHeight = 0;

        // Position children along main and cross axes
        for (DocumentElement child : children) {
            BoxDimensions childDims = child.getDimensions();
            double childTotalH = childDims.getTotalHeight();
            if (childTotalH > maxHeight) {
                maxHeight = childTotalH;
            }

            double posX = currentX + childDims.getMargin().left();
            double posY = startY + childDims.getMargin().top();

            // Align-items cross axis alignment
            switch (style.getAlignItems()) {
                case CENTER -> posY += (maxHeight - childTotalH) / 2.0;
                case FLEX_END -> posY += (maxHeight - childTotalH);
                case STRETCH -> {
                    // stretch content height if desired
                }
                case FLEX_START -> {
                    // stays at top
                }
            }

            childDims.setX(posX);
            childDims.setY(posY);

            currentX += childDims.getTotalWidth() + columnGap + spacing;
        }

        dims.setContentHeight(style.getHeight() >= 0 ? style.getHeight() : maxHeight);
    }

    private void solveFlexColumn(ContainerElement container, double availableWidth) {
        BoxDimensions dims = container.getDimensions();
        ElementStyle style = container.getStyle();
        List<DocumentElement> children = container.getChildren();

        double startX = dims.getContentBoxX();
        double currentY = dims.getContentBoxY();
        double rowGap = style.getRowGap();

        double totalHeight = 0;

        for (int i = 0; i < children.size(); i++) {
            DocumentElement child = children.get(i);
            BoxModelCalculator.initializeDimensions(child);
            BoxDimensions childDims = child.getDimensions();

            double childAvailableW = availableWidth - childDims.getMargin().horizontalTotal();
            double childX = startX + childDims.getMargin().left();
            double childY = currentY + childDims.getMargin().top();

            solve(child, childX, childY, childAvailableW);

            double childTotalH = childDims.getTotalHeight();
            currentY += childTotalH;
            totalHeight += childTotalH;

            if (i < children.size() - 1) {
                currentY += rowGap;
                totalHeight += rowGap;
            }
        }

        dims.setContentHeight(style.getHeight() >= 0 ? style.getHeight() : totalHeight);
    }
}

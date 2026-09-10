package com.engine.paper.engine;

import com.engine.paper.domain.element.ContainerElement;
import com.engine.paper.domain.element.TextBlock;
import com.engine.paper.domain.layout.FlexDirection;
import com.engine.paper.domain.layout.JustifyContent;
import com.engine.paper.domain.model.Margin;
import com.engine.paper.domain.model.Padding;
import com.engine.paper.domain.style.ElementStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class FlexLayoutSolverTest {

    private final FlexLayoutSolver solver = new FlexLayoutSolver();

    @Test
    @DisplayName("Three flex columns with equal flexGrow share parent container width equally")
    void threeEqualFlexColumnsShareWidth() {
        ContainerElement container = new ContainerElement();
        container.getStyle().setDisplay(ElementStyle.Display.FLEX);
        container.getStyle().setFlexDirection(FlexDirection.ROW);

        TextBlock col1 = new TextBlock("Column 1");
        col1.getStyle().setFlexGrow(1.0);

        TextBlock col2 = new TextBlock("Column 2");
        col2.getStyle().setFlexGrow(1.0);

        TextBlock col3 = new TextBlock("Column 3");
        col3.getStyle().setFlexGrow(1.0);

        container.addChild(col1).addChild(col2).addChild(col3);

        double containerWidth = 600.0;
        solver.solve(container, 0, 0, containerWidth);

        assertThat(col1.getDimensions().getBorderBoxWidth()).isCloseTo(200.0, within(1.0));
        assertThat(col2.getDimensions().getBorderBoxWidth()).isCloseTo(200.0, within(1.0));
        assertThat(col3.getDimensions().getBorderBoxWidth()).isCloseTo(200.0, within(1.0));

        assertThat(col1.getDimensions().getX()).isCloseTo(0.0, within(0.1));
        assertThat(col2.getDimensions().getX()).isCloseTo(200.0, within(1.0));
        assertThat(col3.getDimensions().getX()).isCloseTo(400.0, within(1.0));
    }

    @Test
    @DisplayName("Flex row with justify-content: space-between places first at start and last at end")
    void justifyContentSpaceBetween() {
        ContainerElement container = new ContainerElement();
        container.getStyle().setDisplay(ElementStyle.Display.FLEX);
        container.getStyle().setFlexDirection(FlexDirection.ROW);
        container.getStyle().setJustifyContent(JustifyContent.SPACE_BETWEEN);

        TextBlock left = new TextBlock("Left");
        left.getStyle().setWidth(100.0);

        TextBlock right = new TextBlock("Right");
        right.getStyle().setWidth(100.0);

        container.addChild(left).addChild(right);

        double containerWidth = 500.0;
        solver.solve(container, 50, 50, containerWidth);

        // Left element should start at x = 50
        assertThat(left.getDimensions().getX()).isCloseTo(50.0, within(0.1));
        // Right element should end at x = 50 + 500 = 550, meaning its left edge is 450
        assertThat(right.getDimensions().getX()).isCloseTo(450.0, within(0.1));
    }

    @Test
    @DisplayName("Box model padding and margins are correctly accounted for in coordinates")
    void boxModelPaddingAndMarginOffsets() {
        TextBlock block = new TextBlock("Padded Text");
        block.getStyle().setMargin(Margin.symmetric(10, 20));
        block.getStyle().setPadding(Padding.all(15));
        block.getStyle().setWidth(300);

        solver.solve(block, 100, 100, 500);

        assertThat(block.getDimensions().getX()).isEqualTo(100);
        assertThat(block.getDimensions().getY()).isEqualTo(100);
        assertThat(block.getDimensions().getContentBoxX()).isEqualTo(100 + 15);
        assertThat(block.getDimensions().getContentBoxY()).isEqualTo(100 + 15);
        assertThat(block.getDimensions().getTotalWidth()).isEqualTo(300 + 40); // width + margin horizontal total
    }
}

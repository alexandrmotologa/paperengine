package com.engine.paper.domain.layout;

/**
 * Direction of flex container main axis.
 */
public enum FlexDirection {
    ROW,
    COLUMN,
    ROW_REVERSE,
    COLUMN_REVERSE;

    public boolean isRow() {
        return this == ROW || this == ROW_REVERSE;
    }

    public boolean isColumn() {
        return this == COLUMN || this == COLUMN_REVERSE;
    }

    public boolean isReverse() {
        return this == ROW_REVERSE || this == COLUMN_REVERSE;
    }
}

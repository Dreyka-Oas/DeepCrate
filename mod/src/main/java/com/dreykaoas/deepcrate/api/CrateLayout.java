package com.dreykaoas.deepcrate.api;

/**
 * How a crate of {@code rows} rows is cut into pages.
 *
 * Pages share the rows evenly rather than filling one page before the next: eight rows give two
 * pages of four, not one of six and one of two.
 */
public record CrateLayout(int rowsPerPage, int pageCount) {
    public static final int MAX_ROWS_PER_PAGE = 6;

    public static CrateLayout balanced(int rows) {
        int pageCount = Math.max(1, (rows + MAX_ROWS_PER_PAGE - 1) / MAX_ROWS_PER_PAGE);
        int rowsPerPage = (rows + pageCount - 1) / pageCount;
        return new CrateLayout(rowsPerPage, pageCount);
    }

    public CrateLayout {
        if (rowsPerPage < 1 || pageCount < 1) {
            throw new IllegalArgumentException("Crate layout needs at least one row and one page, got " + rowsPerPage + "x" + pageCount);
        }
    }

    public int visibleSlots() {
        return this.rowsPerPage * CrateTier.COLUMNS;
    }
}

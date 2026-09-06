package com.dreykaoas.deepcrate.api;

import com.dreykaoas.deepcrate.config.domain.CrateConfig;

/**
 * How a crate of {@code rows} rows is cut into pages.
 *
 * Pages share the rows evenly rather than filling one page before the next: eight rows give two
 * pages of four, not one of six and one of two.
 */
public record CrateLayout(int rowsPerPage, int pageCount) {
    public static CrateLayout balanced(int rows) {
        // Read once into a local: two reads of the same field in one expression leave a window where
        // the second answers something the first did not.
        int maxRowsPerPage = CrateConfig.maxRowsPerPage;
        int pageCount = Math.max(1, (rows + maxRowsPerPage - 1) / maxRowsPerPage);
        int rowsPerPage = (rows + pageCount - 1) / pageCount;
        return new CrateLayout(rowsPerPage, pageCount);
    }

    public CrateLayout {
        if (rowsPerPage < 1 || pageCount < 1) {
            throw new IllegalArgumentException("Crate layout needs at least one row and one page, got " + rowsPerPage + "x" + pageCount);
        }
    }
}

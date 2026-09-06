package oas.dreyka.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oas.dreyka.deepcrate.api.CrateLayout;
import oas.dreyka.deepcrate.config.domain.CrateConfig;
import org.junit.jupiter.api.Test;

class CrateLayoutTest {
    @Test
    void aSmallCrateFitsOnOnePage() {
        assertEquals(new CrateLayout(3, 1), CrateLayout.balanced(3));
        assertEquals(new CrateLayout(4, 1), CrateLayout.balanced(4));
    }

    @Test
    void pagesShareTheRowsRatherThanFillingTheFirst() {
        // Six rows give two pages of three, not one of four and one of two.
        assertEquals(new CrateLayout(3, 2), CrateLayout.balanced(6));
        assertEquals(new CrateLayout(4, 2), CrateLayout.balanced(8));
    }

    @Test
    void everyRowIsReachableWhateverTheCount() {
        for (int rows = 1; rows <= 40; rows++) {
            CrateLayout crateLayout = CrateLayout.balanced(rows);
            assertTrue(
                crateLayout.rowsPerPage() * crateLayout.pageCount() >= rows,
                "rows " + rows + " left some slots unreachable: " + crateLayout
            );
            assertTrue(crateLayout.rowsPerPage() <= CrateConfig.maxRowsPerPage, "rows " + rows + " overflowed a page");
        }
    }

    @Test
    void noPageIsLeftEntirelyEmpty() {
        for (int rows = 1; rows <= 40; rows++) {
            CrateLayout crateLayout = CrateLayout.balanced(rows);
            int rowsBeforeLastPage = crateLayout.rowsPerPage() * (crateLayout.pageCount() - 1);
            assertTrue(rowsBeforeLastPage < rows, "rows " + rows + " produced an empty last page: " + crateLayout);
        }
    }
}

package ch.sbb.polarion.extension.diff_tool.widgets;

import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSetWidgetParamsTest {

    @Mock
    private DataSet dataSet;

    @Mock
    private IterableWithSize<ModelObject> items;

    @Test
    void testDataSetClampsCurrentPageToMinimum() {
        when(dataSet.items()).thenReturn(items);
        when(items.size()).thenReturn(100);

        DataSetWidgetParams params = DataSetWidgetParams.builder()
                .currentPage(0) // Below minimum
                .recordsPerPage(10)
                .build();

        params.dataSet(dataSet);

        assertEquals(1, params.currentPage(), "Current page should be clamped to minimum 1");
        assertEquals(10, params.lastPage());
    }

    @Test
    void testDataSetClampsCurrentPageToMaximum() {
        when(dataSet.items()).thenReturn(items);
        when(items.size()).thenReturn(50);

        DataSetWidgetParams params = DataSetWidgetParams.builder()
                .currentPage(100) // Above maximum
                .recordsPerPage(10)
                .build();

        params.dataSet(dataSet);

        assertEquals(5, params.currentPage(), "Current page should be clamped to last page");
        assertEquals(5, params.lastPage());
    }

    @Test
    void testDataSetKeepsValidCurrentPage() {
        when(dataSet.items()).thenReturn(items);
        when(items.size()).thenReturn(100);

        DataSetWidgetParams params = DataSetWidgetParams.builder()
                .currentPage(5) // Valid page
                .recordsPerPage(10)
                .build();

        params.dataSet(dataSet);

        assertEquals(5, params.currentPage(), "Current page should remain unchanged when valid");
        assertEquals(10, params.lastPage());
    }

    @Test
    void testDataSetWithEmptyItems() {
        when(dataSet.items()).thenReturn(items);
        when(items.size()).thenReturn(0);

        DataSetWidgetParams params = DataSetWidgetParams.builder()
                .currentPage(5)
                .recordsPerPage(10)
                .build();

        params.dataSet(dataSet);

        assertEquals(1, params.currentPage(), "Current page should be 1 for empty dataset");
        assertEquals(1, params.lastPage(), "Last page should be 1 for empty dataset");
    }
}

package ch.sbb.polarion.extension.diff_tool.widgets;

import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
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

    @Test
    void testSideEnumValues() {
        DataSetWidgetParams.Side[] values = DataSetWidgetParams.Side.values();

        assertEquals(2, values.length);
        assertEquals(DataSetWidgetParams.Side.SOURCE, values[0]);
        assertEquals(DataSetWidgetParams.Side.TARGET, values[1]);
    }

    @Test
    void testSideEnumToString() {
        assertEquals("source", DataSetWidgetParams.Side.SOURCE.toString());
        assertEquals("target", DataSetWidgetParams.Side.TARGET.toString());
    }

    @Test
    void testSideEnumValueOf() {
        assertEquals(DataSetWidgetParams.Side.SOURCE, DataSetWidgetParams.Side.valueOf("SOURCE"));
        assertEquals(DataSetWidgetParams.Side.TARGET, DataSetWidgetParams.Side.valueOf("TARGET"));
    }

    @Test
    void testFromRequestWithSourceSide() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("sourceProjectId")).thenReturn("testProject");
        when(request.getParameter("sourceQuery")).thenReturn("type:req");
        when(request.getParameter("sourcePage")).thenReturn("3");
        when(request.getParameter("sourceRecordsPerPage")).thenReturn("25");

        DataSetWidgetParams params = DataSetWidgetParams.fromRequest(request, DataSetWidgetParams.Side.SOURCE);

        assertEquals(DataSetWidgetParams.Side.SOURCE, params.side());
        assertEquals("testProject", params.projectId());
        assertEquals("type:req", params.query());
        assertEquals(3, params.currentPage());
        assertEquals(25, params.recordsPerPage());
    }

    @Test
    void testFromRequestWithTargetSide() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("targetProjectId")).thenReturn("anotherProject");
        when(request.getParameter("targetQuery")).thenReturn("type:task");
        when(request.getParameter("targetPage")).thenReturn("2");
        when(request.getParameter("targetRecordsPerPage")).thenReturn("50");

        DataSetWidgetParams params = DataSetWidgetParams.fromRequest(request, DataSetWidgetParams.Side.TARGET);

        assertEquals(DataSetWidgetParams.Side.TARGET, params.side());
        assertEquals("anotherProject", params.projectId());
        assertEquals("type:task", params.query());
        assertEquals(2, params.currentPage());
        assertEquals(50, params.recordsPerPage());
    }

    @Test
    void testFromRequestWithDefaultValues() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("sourceProjectId")).thenReturn(null);
        when(request.getParameter("sourceQuery")).thenReturn(null);
        when(request.getParameter("sourcePage")).thenReturn(null);
        when(request.getParameter("sourceRecordsPerPage")).thenReturn(null);

        DataSetWidgetParams params = DataSetWidgetParams.fromRequest(request, DataSetWidgetParams.Side.SOURCE);

        assertEquals(DataSetWidgetParams.Side.SOURCE, params.side());
        assertNull(params.projectId());
        assertNull(params.query());
        assertEquals(1, params.currentPage());
        assertEquals(20, params.recordsPerPage());
    }

    @Test
    void testFromRequestWithInvalidNumbers() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("sourceProjectId")).thenReturn(null);
        when(request.getParameter("sourceQuery")).thenReturn(null);
        when(request.getParameter("sourcePage")).thenReturn("invalid");
        when(request.getParameter("sourceRecordsPerPage")).thenReturn("not_a_number");

        DataSetWidgetParams params = DataSetWidgetParams.fromRequest(request, DataSetWidgetParams.Side.SOURCE);

        assertEquals(1, params.currentPage(), "Invalid page number should default to 1");
        assertEquals(20, params.recordsPerPage(), "Invalid records per page should default to 20");
    }

    @Test
    void testBuilderWithAllFields() {
        DataSetWidgetParams params = DataSetWidgetParams.builder()
                .side(DataSetWidgetParams.Side.SOURCE)
                .projectId("proj1")
                .query("type:req")
                .currentPage(5)
                .lastPage(10)
                .recordsPerPage(15)
                .build();

        assertEquals(DataSetWidgetParams.Side.SOURCE, params.side());
        assertEquals("proj1", params.projectId());
        assertEquals("type:req", params.query());
        assertEquals(5, params.currentPage());
        assertEquals(10, params.lastPage());
        assertEquals(15, params.recordsPerPage());
    }

    @Test
    void testDataSetSetsItems() {
        when(dataSet.items()).thenReturn(items);
        when(items.size()).thenReturn(100);

        DataSetWidgetParams params = DataSetWidgetParams.builder()
                .currentPage(1)
                .recordsPerPage(10)
                .build();

        params.dataSet(dataSet);

        assertNotNull(params.items());
        assertEquals(items, params.items());
        assertEquals(dataSet, params.dataSet());
    }
}

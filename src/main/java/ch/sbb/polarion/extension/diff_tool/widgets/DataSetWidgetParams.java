package ch.sbb.polarion.extension.diff_tool.widgets;

import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

@Data
@Builder
@Accessors(fluent = true)
public class DataSetWidgetParams {

    public enum Side {
        SOURCE, TARGET;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private static final String PROJECT_ID_PARAM = "ProjectId";
    private static final String QUERY_PARAM = "Query";
    private static final String PAGE_PARAM = "Page";
    private static final String RECORDS_PER_PAGE_PARAM = "RecordsPerPage";

    private Side side;
    private String projectId;
    private String query;
    private int currentPage;
    private int lastPage;
    private int recordsPerPage;
    private DataSet dataSet;
    private IterableWithSize<ModelObject> items;

    public void dataSet(@NotNull DataSet dataSet) {
        this.dataSet = dataSet;

        items = dataSet.items();

        lastPage = (Math.max(0, items.size() - 1) / recordsPerPage) + 1;
        currentPage = Math.clamp(currentPage, 1, lastPage);
    }

    public static DataSetWidgetParams fromRequest(@NotNull HttpServletRequest request, @NotNull Side side) {
        return DataSetWidgetParams.builder()
                .side(side)
                .projectId(getParameter(request, side, PROJECT_ID_PARAM))
                .query(getParameter(request, side, QUERY_PARAM))
                .currentPage(parseNumberSafely(getParameter(request, side, PAGE_PARAM), 1))
                .recordsPerPage(parseNumberSafely(getParameter(request, side, RECORDS_PER_PAGE_PARAM), 20))
                .build();
    }

    private static String getParameter(@NotNull HttpServletRequest request, @NotNull Side side, @NotNull String paramSuffix) {
        return request.getParameter(side + paramSuffix);
    }

    private static int parseNumberSafely(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}

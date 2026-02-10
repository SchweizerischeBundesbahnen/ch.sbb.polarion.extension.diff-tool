package ch.sbb.polarion.extension.diff_tool.widgets;

import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.impl.ScopeImpl;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.Field;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.FieldsParameterImpl;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.SortingParameterImpl;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.portal.internal.shared.navigation.ProjectScope;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CollectionsDiffWidgetRenderer extends DiffWidgetRenderer {
    private static final String WIDGET_ID = "collections-diff-widget";

    private static final FieldsParameter COLUMNS_PARAMETER = new FieldsParameterImpl.Builder(WIDGET_ID)
            .fields(List.of(IBaselineCollection.KEY_NAME, IBaselineCollection.KEY_AUTHOR, IBaselineCollection.KEY_CREATED, IBaselineCollection.KEY_UPDATED)).build();

    private final SortingParameter sortingParameter = new SortingParameterImpl.Builder(WIDGET_ID).byLuceneSortString(IBaselineCollection.KEY_NAME).build();

    private final DiffWidgetParams params;

    public CollectionsDiffWidgetRenderer(@NotNull RichPageWidgetCommonContext context, @NotNull DiffWidgetParams params) {
        this(context, params, new PolarionService());
    }

    public CollectionsDiffWidgetRenderer(@NotNull RichPageWidgetCommonContext context, @NotNull DiffWidgetParams params, @NotNull PolarionService polarionService) {
        super(context, COLUMNS_PARAMETER, params, polarionService);

        this.params = params;
    }

    @Override
    protected void render(@NotNull HtmlFragmentBuilder builder) {
        HtmlTagBuilder wrap = builder.tag().div();
        wrap.attributes().className("main-pane");

        HtmlTagBuilder topPane = wrap.append().tag().div();
        renderCompareButton(topPane,
                "Please, select one item in left table and one item in right table to be compared");

        HtmlTagBuilder description = topPane.append().tag().div();
        description.append().tag().p().append().text(
                "Please select one collection in left table and then one collection in right table below which you want to compare and click button above");

        renderLinkRole(wrap);
        renderConfiguration(wrap);

        HtmlTagBuilder columns = wrap.append().tag().div();
        columns.attributes().className("columns");

        HtmlTagBuilder sourceColumn = columns.append().tag().div();
        sourceColumn.attributes().className("items-for-diff").className("column");

        renderQueryPanel(sourceColumn, params.sourceParams());
        if (!context.getDisplayedScope().isGlobal()) {
            DataSet dataSet = initDataSet(context.getDisplayedScope(), params.sourceParams().query());
            params.sourceParams().dataSet(dataSet);
        }
        renderTable(sourceColumn, params.sourceParams());

        HtmlTagBuilder targetColumn = columns.append().tag().div();
        targetColumn.attributes().className("items-for-diff").className("column");

        renderQueryPanel(targetColumn, params.targetParams()); // This method has a side effect on targetParams parameter, initializing projectId attribute if it's empty
        if (!StringUtils.isBlank(params.targetParams().projectId())) {
            ProjectScope projectScope = new ProjectScope(params.targetParams().projectId());
            DataSet dataSet = initDataSet(new ScopeImpl(projectScope), params.targetParams().query());
            params.targetParams().dataSet(dataSet);
        }
        renderTable(targetColumn, params.targetParams());
    }

    private DataSet initDataSet(@NotNull Scope scope, @Nullable String query) {
        return initDataSet(WIDGET_ID, PrototypeEnum.BaselineCollection, scope, sortingParameter, query);
    }

    private void renderTable(@NotNull HtmlTagBuilder parent, @NotNull DataSetWidgetParams dataSetParams) {
        HtmlTagBuilder outerTable = parent.append().tag().table();
        outerTable.attributes().className("polarion-rpw-table-main");
        //language=JS
        outerTable.attributes().onClick(String.format("DiffToolWidgetUtils.updateCollectionsDiffButton(this, '%s');", params.sourceParams().projectId()));

        HtmlTagBuilder contentTable = outerTable.append().tag().tr().append().tag().td().append().tag().table();
        contentTable.attributes().className("polarion-rpw-table-content");

        renderHeaderRow(contentTable.append());

        renderContentRows(contentTable.append(), dataSetParams);

        this.renderFooter(outerTable.append().tag().tr().append().tag().td(), dataSetParams);
    }

    protected void renderItem(@NotNull HtmlContentBuilder builder, @NotNull ModelObject item, @NotNull DataSetWidgetParams dataSetParams) {
        if (item.isUnresolvable()) {
            this.renderNotReadableRow(builder, this.localization.getString("richpages.widget.table.unresolvableItem", item.getReferenceToCurrent().toPath()));
        } else if (!item.can().read()) {
            this.renderNotReadableRow(builder, this.localization.getString("security.cannotread"));
        } else {
            HtmlTagBuilder td = builder.tag().td();
            HtmlTagBuilder checkbox = td.append().tag().byName("input");
            checkbox.attributes()
                    .byName("type", "radio")
                    .byName("name", dataSetParams.side().toString() + "-collection")
                    .byName("data-type", item.getOldApi().getPrototype().getName())
                    .byName("data-id", getValue(item, "id"))
                    .className("export-item");

            for (Field column : getFields()) {
                td = builder.tag().td();
                column.render(item.fields()).withLinks(true).htmlTo(td.append());
            }
        }
    }

}

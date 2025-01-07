package ch.sbb.polarion.extension.diff_tool.widgets;

import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.Field;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.FieldsParameterImpl;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.SortingParameterImpl;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.IWorkItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WorkItemsDiffWidgetRenderer extends DiffWidgetRenderer {
    private static final String WIDGET_ID = "work-items-diff-widget";

    private static final FieldsParameter COLUMNS_PARAMETER = new FieldsParameterImpl.Builder(WIDGET_ID)
            .fields(List.of(IWorkItem.KEY_ID, IWorkItem.KEY_TITLE, IWorkItem.KEY_TYPE, IWorkItem.KEY_STATUS, IWorkItem.KEY_SEVERITY))
            .build();

    private final DiffWidgetParams params;

    public WorkItemsDiffWidgetRenderer(@NotNull RichPageWidgetCommonContext context, @NotNull DiffWidgetParams params) {
        super(context, COLUMNS_PARAMETER, params);

        this.params = params;
        if (!context.getDisplayedScope().isGlobal()) {
            SortingParameter sortingParameter = new SortingParameterImpl.Builder(WIDGET_ID).byLuceneSortString(IWorkItem.KEY_ID).build();
            params.sourceParams().dataSet(initDataSet(WIDGET_ID, PrototypeEnum.WorkItem, context.getDisplayedScope(), sortingParameter, params.sourceParams().query()));
        }
    }

    protected void render(@NotNull HtmlFragmentBuilder builder) {
        HtmlTagBuilder wrap = builder.tag().div();
        wrap.attributes().className("main-pane");

        HtmlTagBuilder topPane = wrap.append().tag().div();
        renderCompareButton(topPane, "Please, select at least one item to be compared");

        HtmlTagBuilder description = topPane.append().tag().div();
        description.append().tag().p().append().text("Please select diff configuration, link role and work items below which you want to compare and click button above");

        renderTargetProject(wrap, params.targetParams());
        renderLinkRole(wrap);
        renderConfiguration(wrap);

        HtmlTagBuilder diffItems = wrap.append().tag().div();
        diffItems.attributes().className("items-for-diff");

        renderQueryPanel(diffItems, params.sourceParams());

        HtmlTagBuilder outerTable = diffItems.append().tag().table();

        outerTable.attributes().className("polarion-rpw-table-main");
        //language=JS
        outerTable.attributes().onClick(String.format("DiffTool.updateWorkItemsDiffButton(this, '%s');", params.sourceParams().projectId()));

        HtmlTagBuilder contentTable = outerTable.append().tag().tr().append().tag().td().append().tag().table();
        contentTable.attributes().className("polarion-rpw-table-content");

        renderHeaderRow(contentTable.append(), true);

        renderContentRows(contentTable.append(), params.sourceParams());

        this.renderFooter(outerTable.append().tag().tr().append().tag().td(), params.sourceParams());
    }

    private String getSpace(@NotNull ModelObject item) {
        String spaceFieldId = null;
        if (item.getOldApi() instanceof IModule) {
            spaceFieldId = "moduleFolder";
        } else if (item.getOldApi() instanceof IRichPage) {
            spaceFieldId = "spaceId";
        }
        if (spaceFieldId != null) {
            return getValue(item, spaceFieldId);
        } else {
            return "";
        }
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
                    .byName("type", "checkbox")
                    .byName("data-type", item.getOldApi().getPrototype().getName())
                    .byName("data-space", getSpace(item))
                    .byName("data-id", getValue(item, "id"))
                    .className("export-item");

            for (Field column : getFields()) {
                td = builder.tag().td();
                column.render(item.fields()).withLinks(true).htmlTo(td.append());
            }
        }
    }
}

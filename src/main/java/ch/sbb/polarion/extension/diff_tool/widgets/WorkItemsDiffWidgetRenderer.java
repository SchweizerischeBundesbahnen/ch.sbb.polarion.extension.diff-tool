package ch.sbb.polarion.extension.diff_tool.widgets;

import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.impl.HierarchicalUrlParametersResolver;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.DataSetParameterBuilder;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.server.api.model.rp.widget.BottomQueryLinksBuilder;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.Field;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.DataSetParameterImpl;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.FieldsParameterImpl;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.SortingParameterImpl;
import com.polarion.alm.shared.api.model.rp.parameter.impl.init.HtmlRichPageParameterInitializer;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.ui.shared.LinearGradientColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class WorkItemsDiffWidgetRenderer extends AbstractWidgetRenderer {
    private static final String WIDGET_ID = "work-items-diff-widget";

    @NotNull
    private final IterableWithSize<Field> columns;
    @Nullable
    private DataSet dataSet;
    @Nullable
    private IterableWithSize<ModelObject> items;
    private String projectId;
    private final String targetProject;
    private final String linkRole;
    private final String configuration;
    private final String query;
    private int recordsPerPage = 20;
    private int currentPage;
    private int lastPage;

    PolarionService polarionService = new PolarionService();

    public WorkItemsDiffWidgetRenderer(@NotNull RichPageWidgetCommonContext context, @NotNull WorkItemsDiffWidgetParams params) {
        super(context);

        FieldsParameter columnsParameter = new FieldsParameterImpl.Builder(WIDGET_ID).fields(List.of(IWorkItem.KEY_ID, IWorkItem.KEY_TITLE, IWorkItem.KEY_TYPE, IWorkItem.KEY_STATUS, IWorkItem.KEY_SEVERITY)).build();
        this.columns = columnsParameter.fields();
        this.targetProject = params.targetProject();
        this.linkRole = params.linkRole();
        this.configuration = params.configuration();
        this.query = params.query();
        try {
            this.recordsPerPage = Integer.parseInt(params.recordsPerPage());
        } catch (NumberFormatException ex) {
            // Just ignore
        }
        try {
            currentPage = Integer.parseInt(params.page());
        } catch (NumberFormatException ex) {
            // Just ignore
        }

        if (!context.getDisplayedScope().isGlobal()) {
            projectId = context.getDisplayedScope().projectId();
            SortingParameter sortingParameter = new SortingParameterImpl.Builder(WIDGET_ID).byLuceneSortString(IWorkItem.KEY_ID).build();
            DataSetParameterImpl dataSetParameter = (DataSetParameterImpl) new DataSetParameterBuilder(WIDGET_ID)
                    .allowedPrototypes(PrototypeEnum.WorkItem)
                    .add("columns", columnsParameter)
                    .add("sortBy", sortingParameter)
                    .dependencySource(true)
                    .luceneQuery(query)
                    .build();
            dataSetParameter.initialize(new HtmlRichPageParameterInitializer(null, true,
                    context.getDisplayedScope(), new StrictMapImpl<>(), new HierarchicalUrlParametersResolver()));
            dataSetParameter.setRenderingContext(context);

            String sort = sortingParameter.asLuceneSortString();
            dataSet = dataSetParameter.getFor().sort(sort).revision(null);
            items = dataSet.items();

            lastPage = (Math.max(0, items.size() - 1) / recordsPerPage) + 1;
            currentPage = Math.min(Math.max(1, currentPage), lastPage); // First check that page number can't be less than 1, then check it doesn't exceed last page
        }
    }

    protected void render(@NotNull HtmlFragmentBuilder builder) {
        HtmlTagBuilder wrap = builder.tag().div();
        wrap.attributes().className("main-pane");

        HtmlTagBuilder topPane = wrap.append().tag().div();
        renderCompareButton(topPane);

        HtmlTagBuilder description = topPane.append().tag().div();
        description.append().tag().p().append().text("Please select diff configuration, link role and work items below which you want to compare and click button above");

        renderTargetProject(wrap);
        renderLinkRole(wrap);
        renderConfiguration(wrap);

        HtmlTagBuilder diffItems = wrap.append().tag().div();
        diffItems.attributes().className("items-for-diff");

        renderQueryPanel(diffItems);

        HtmlTagBuilder mainTable = diffItems.append().tag().table();

        mainTable.attributes().className("polarion-rpw-table-main");
        //language=JS
        mainTable.attributes().onClick(String.format("DiffTool.updateWorkItemsDiffButton(this, '%s');", projectId));

        HtmlContentBuilder contentBuilder = mainTable.append();
        this.renderContentTable(contentBuilder.tag().tr().append().tag().td().append());
        this.renderFooter(contentBuilder.tag().tr().append().tag().td());
    }

    private void renderCompareButton(@NotNull HtmlTagBuilder header) {
        LinearGradientColor color = new LinearGradientColor();
        HtmlTagBuilder buttonSpan = header.append().tag().span();
        buttonSpan.attributes().className("polarion-TestsExecutionButton-link").title("Please, select at least one item to be compared first");

        HtmlTagBuilder a = buttonSpan.append().tag().a();
        HtmlTagBuilder button = a.append().tag().div();
        button.attributes().className("polarion-TestsExecutionButton-buttons polarion-TestsExecutionButton-buttons-defaultCursor").style(color.getStyle());

        HtmlTagBuilder content = button.append().tag().table();
        content.attributes().className("polarion-TestsExecutionButton-buttons-content");

        HtmlTagBuilder labelCell = content.append().tag().tr().append().tag().td();
        labelCell.attributes().className("polarion-TestsExecutionButton-buttons-content-labelCell");
        HtmlTagBuilder label = labelCell.append().tag().div();
        label.attributes().className("polarion-TestsExecutionButton-labelTextNew");

        label.append().text("Compare");
    }

    private void renderTargetProject(@NotNull HtmlTagBuilder parent) {
        HtmlTagBuilder targetProjectPane = parent.append().tag().div();
        targetProjectPane.attributes().className("target-project");

        targetProjectPane.append().tag().byName("label").append().text("Target project:");
        HtmlTagBuilder targetProjectSelect = targetProjectPane.append().tag().byName("select");
        targetProjectSelect.attributes().id("comparison-target-project-selector");

        List<IProject> projects = polarionService.getProjects();
        for (IProject project : projects) {
            HtmlTagBuilder option = targetProjectSelect.append().tag().byName("option");
            option.attributes().byName("value", project.getId());
            option.append().text(project.getName());
            if (project.getId().equals(this.targetProject)) {
                option.attributes().booleanAttribute("selected");
            }
        }
    }

    private void renderLinkRole(@NotNull HtmlTagBuilder parent) {
        HtmlTagBuilder linkRolePane = parent.append().tag().div();
        linkRolePane.attributes().className("link-role");

        linkRolePane.append().tag().byName("label").append().text("Link role:");
        HtmlTagBuilder linkRoleSelect = linkRolePane.append().tag().byName("select");
        linkRoleSelect.attributes().id("comparison-link-role-selector");

        Collection<ILinkRoleOpt> linkRoles = polarionService.getLinkRoles(projectId);
        for (ILinkRoleOpt linkRole : linkRoles) {
            HtmlTagBuilder option = linkRoleSelect.append().tag().byName("option");
            option.attributes().byName("value", linkRole.getId());
            option.append().text(String.format("%s / %s", linkRole.getName(), linkRole.getOppositeName()));
            if (linkRole.getId().equals(this.linkRole)) {
                option.attributes().booleanAttribute("selected");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void renderConfiguration(@NotNull HtmlTagBuilder parent) {
        HtmlTagBuilder configurationPane = parent.append().tag().div();
        configurationPane.attributes().className("configuration");

        configurationPane.append().tag().byName("label").append().text("Configuration:");
        HtmlTagBuilder configurationSelect = configurationPane.append().tag().byName("select");
        configurationSelect.attributes().id("comparison-config-selector");

        Collection<SettingName> configurations = NamedSettingsRegistry.INSTANCE.getByFeatureName(DiffSettings.FEATURE_NAME).readNames(ScopeUtils.getScopeFromProject(projectId));
        for (SettingName configuration : configurations) {
            HtmlTagBuilder option = configurationSelect.append().tag().byName("option");
            option.attributes().byName("value", configuration.getName());
            option.append().text(configuration.getName());
            if (configuration.getId().equals(this.configuration)) {
                option.attributes().booleanAttribute("selected");
            }
        }
    }

    private void renderQueryPanel(@NotNull HtmlTagBuilder parent) {
        HtmlTagBuilder queryPane = parent.append().tag().div();
        queryPane.attributes().className("query");

        queryPane.append().tag().byName("label").append().text("Query (Lucene):");
        HtmlTagBuilder queryInput = queryPane.append().tag().byName("input");
        queryInput.attributes().id("query-input").byName("value", this.query);

        HtmlTagBuilder recordsPerPageLabel = queryPane.append().tag().byName("label");
        recordsPerPageLabel.attributes().className("records-per-page-label");
        recordsPerPageLabel.append().text("Records per page:");

        HtmlTagBuilder recordsPerPageInput = queryPane.append().tag().byName("input");
        recordsPerPageInput.attributes().id("records-per-page-input").byName("value", String.valueOf(this.recordsPerPage));

        HtmlTagBuilder applyButton = queryPane.append().tag().byName("button");
        applyButton.append().text("Apply");
        //language=JS
        applyButton.attributes().onClick("""
                const newValue = document.getElementById('query-input').value;
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProject', document.getElementById('comparison-target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('comparison-link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('comparison-config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'query', newValue);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'recordsPerPage', document.getElementById('records-per-page-input').value);
                top.location.reload()""");

        HtmlTagBuilder resetButton = queryPane.append().tag().byName("button");
        resetButton.append().text("Reset");
        //language=JS
        resetButton.attributes().onClick("""
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProject', document.getElementById('comparison-target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('comparison-link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('comparison-config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'query', '');
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'recordsPerPage', document.getElementById('records-per-page-input').value);
                top.location.reload()""");

    }

    private void renderContentTable(@NotNull HtmlContentBuilder builder) {
        HtmlTagBuilder table = builder.tag().table();
        table.attributes().className("polarion-rpw-table-content");
        this.renderContentRows(table.append());
    }

    private void renderContentRows(@NotNull HtmlContentBuilder builder) {
        this.renderHeaderRow(builder);
        if (items == null) {
            return;
        }

        List<ModelObject> itemsList = items.toArrayList();
        int pageEnd = currentPage * recordsPerPage; // Potential end based on page number and page size
        int pageStart = pageEnd - recordsPerPage;
        pageEnd = Math.min(pageEnd, items.size()); // Recalculate not to go out of boundaries
        for (int i = pageStart; i < pageEnd; i++) {
            HtmlTagBuilder tr = builder.tag().tr();
            tr.attributes().className("polarion-rpw-table-content-row");
            this.renderItem(tr.append(), itemsList.get(i));
        }
    }

    private void renderItem(@NotNull HtmlContentBuilder builder, @NotNull ModelObject item) {
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

            for (Field column : this.columns) {
                td = builder.tag().td();
                column.render(item.fields()).withLinks(true).htmlTo(td.append());
            }
        }
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

    private String getValue(@NotNull ModelObject item, @NotNull String fieldName) {
        Object fieldValue = item.getOldApi().getValue(fieldName);
        return fieldValue == null ? "" : fieldValue.toString();
    }

    private void renderNotReadableRow(@NotNull HtmlContentBuilder builder, @NotNull String message) {
        HtmlTagBuilder td = builder.tag().td();
        td.attributes().colspan("" + (this.columns.size() + 1)).className("polarion-rpw-table-not-readable-cell");
        td.append().text(message);
    }

    private void renderHeaderRow(@NotNull HtmlContentBuilder builder) {
        HtmlTagBuilder row = builder.tag().tr();
        row.attributes().className("polarion-rpw-table-header-row");
        HtmlTagBuilder th = row.append().tag().th();
        HtmlTagBuilder checkbox = th.append().tag().byName("input");
        checkbox.attributes().byName("type", "checkbox").className("export-all");
        //language=JS
        checkbox.attributes().onClick("DiffTool.selectAllItems(this);");

        for (Field column : this.columns) {
            row.append().tag().th().append().text(column.label());
        }
    }

    private void renderFooter(@NotNull HtmlTagBuilder tagBuilder) {
        if (dataSet != null) {
            (new BottomQueryLinksBuilder(context, dataSet, recordsPerPage)).render(tagBuilder);

            if (lastPage > 1) {
                renderPaginator(tagBuilder);
            }
        }
    }

    private void renderPaginator(@NotNull HtmlTagBuilder tagBuilder) {
        HtmlTagBuilder paginator = tagBuilder.append().tag().div();
        paginator.attributes().className("paginator");

        if (currentPage > 1) {
            renderPaginatorLink(paginator, "<<", 1);
            renderPaginatorLink(paginator, "<", currentPage - 1);

            if (currentPage - 3 > 0) {
                paginator.append().tag().span().append().text("...");
            }

            int prevPrevPage = currentPage - 2;
            if (prevPrevPage > 0) {
                renderPaginatorLink(paginator, String.valueOf(prevPrevPage), prevPrevPage);
            }

            renderPaginatorLink(paginator, String.valueOf(currentPage - 1), currentPage - 1);
        }

        HtmlTagBuilder currentSpan = paginator.append().tag().span();
        currentSpan.append().text(String.valueOf(currentPage));

        if (currentPage < lastPage) {
            renderPaginatorLink(paginator, String.valueOf(currentPage + 1), currentPage + 1);

            int nextNextPage = currentPage + 2;
            if (nextNextPage <= lastPage) {
                renderPaginatorLink(paginator, String.valueOf(nextNextPage), nextNextPage);
            }

            if (currentPage + 3 <= lastPage) {
                paginator.append().tag().span().append().text("...");
            }

            renderPaginatorLink(paginator, ">", currentPage + 1);
            renderPaginatorLink(paginator, ">>", lastPage);
        }
    }

    private void renderPaginatorLink(@NotNull HtmlTagBuilder paginator, @NotNull String text, int page) {
        HtmlTagBuilder link = paginator.append().tag().a();
        link.append().text(text);
        //language=JS
        String script = """
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProject', document.getElementById('comparison-target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('comparison-link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('comparison-config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'recordsPerPage', document.getElementById('records-per-page-input').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'page', '<PAGE_NUMBER>');
                top.location.reload()""";
        link.attributes().onClick(script.replace("<PAGE_NUMBER>", String.valueOf(page)));
    }
}

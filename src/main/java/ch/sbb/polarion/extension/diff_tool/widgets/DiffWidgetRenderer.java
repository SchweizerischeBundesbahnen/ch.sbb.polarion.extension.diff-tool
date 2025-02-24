package ch.sbb.polarion.extension.diff_tool.widgets;

import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.server.api.model.rp.widget.BottomQueryLinksBuilder;
import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.Field;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.HierarchicalUrlParametersResolver;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.DataSetParameterBuilder;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.DataSetParameterImpl;
import com.polarion.alm.shared.api.model.rp.parameter.impl.init.HtmlRichPageParameterInitializer;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.ui.shared.LinearGradientColor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;

abstract class DiffWidgetRenderer extends AbstractWidgetRenderer {

    private final PolarionService polarionService;
    private final FieldsParameter columnsParameter;
    private final DiffWidgetParams diffWidgetParams;

    protected DiffWidgetRenderer(@NotNull RichPageWidgetCommonContext context, @NotNull FieldsParameter columnsParameter, @NotNull DiffWidgetParams diffWidgetParams) {
        super(context);
        this.polarionService = new PolarionService();
        this.columnsParameter = columnsParameter;
        this.diffWidgetParams = diffWidgetParams;
    }

    @VisibleForTesting
    DiffWidgetRenderer(@NotNull RichPageWidgetCommonContext context, @NotNull FieldsParameter columnsParameter, @NotNull DiffWidgetParams diffWidgetParams, @NotNull PolarionService polarionService) {
        super(context);
        this.polarionService = polarionService;
        this.columnsParameter = columnsParameter;
        this.diffWidgetParams = diffWidgetParams;
    }

    protected String getProjectId() {
        return diffWidgetParams.sourceParams().projectId();
    }

    protected String getLinkRole() {
        return diffWidgetParams.linkRole();
    }

    protected String getConfiguration() {
        return diffWidgetParams.configuration();
    }

    @VisibleForTesting
    DataSet initDataSet(@NotNull String widgetId, @NotNull PrototypeEnum allowedPrototype, @NotNull Scope scope,
                                  @NotNull SortingParameter sortingParameter, @Nullable String query) {
        DataSetParameterImpl dataSetParameter = (DataSetParameterImpl) new DataSetParameterBuilder(widgetId)
                .allowedPrototypes(allowedPrototype)
                .add("sortBy", sortingParameter)
                .dependencySource(true)
                .luceneQuery(query)
                .build();
        dataSetParameter.initialize(new HtmlRichPageParameterInitializer(null, true,
                scope, new StrictMapImpl<>(), new HierarchicalUrlParametersResolver()));
        dataSetParameter.setRenderingContext(context);

        return dataSetParameter.getFor().sort(sortingParameter.asLuceneSortString()).revision(null);
    }

    protected IterableWithSize<Field> getFields() {
        return columnsParameter.fields();
    }

    protected void renderCompareButton(@NotNull HtmlTagBuilder parent, @Nullable String buttonTitle) {
        LinearGradientColor color = new LinearGradientColor();
        HtmlTagBuilder buttonSpan = parent.append().tag().span();
        buttonSpan.attributes().className("polarion-TestsExecutionButton-link").title(buttonTitle);

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

    protected void renderLinkRole(@NotNull HtmlTagBuilder parent) {
        HtmlTagBuilder linkRolePane = parent.append().tag().div();
        linkRolePane.attributes().className("link-role");

        linkRolePane.append().tag().byName("label").append().text("Link role:");
        HtmlTagBuilder linkRoleSelect = linkRolePane.append().tag().byName("select");
        linkRoleSelect.attributes().id("link-role-selector");

        if (StringUtils.isNotBlank(getProjectId())) {
            Collection<ILinkRoleOpt> linkRoles = polarionService.getLinkRoles(getProjectId());
            for (ILinkRoleOpt linkRole : linkRoles) {
                HtmlTagBuilder option = linkRoleSelect.append().tag().byName("option");
                option.attributes().byName("value", linkRole.getId());
                option.append().text(String.format("%s / %s", linkRole.getName(), linkRole.getOppositeName()));
                if (linkRole.getId().equals(this.getLinkRole())) {
                    option.attributes().booleanAttribute("selected");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void renderConfiguration(@NotNull HtmlTagBuilder parent) {
        HtmlTagBuilder configurationPane = parent.append().tag().div();
        configurationPane.attributes().className("configuration");

        configurationPane.append().tag().byName("label").append().text("Configuration:");
        HtmlTagBuilder configurationSelect = configurationPane.append().tag().byName("select");
        configurationSelect.attributes().id("config-selector");

        Collection<SettingName> configurations = NamedSettingsRegistry.INSTANCE.getByFeatureName(DiffSettings.FEATURE_NAME).readNames(ScopeUtils.getScopeFromProject(getProjectId()));
        for (SettingName configuration : configurations) {
            HtmlTagBuilder option = configurationSelect.append().tag().byName("option");
            option.attributes().byName("value", configuration.getName());
            option.append().text(configuration.getName());
            if (configuration.getName().equals(this.getConfiguration())) {
                option.attributes().booleanAttribute("selected");
            }
        }
    }

    protected void renderQueryPanel(@NotNull HtmlTagBuilder parent, @NotNull DataSetWidgetParams dataSetParams) {
        HtmlTagBuilder queryPane = parent.append().tag().div();
        queryPane.attributes().className("query");

        if (dataSetParams.side() == DataSetWidgetParams.Side.TARGET) {
            renderTargetProject(queryPane, dataSetParams); // This method has a side effect on params parameter, initializing projectId attribute if it's empty
        }

        HtmlTagBuilder queryLabel = queryPane.append().tag().byName("label");
        queryLabel.append().text("Query (Lucene):");
        HtmlTagBuilder queryInput = queryPane.append().tag().byName("input");
        queryInput.attributes().id(dataSetParams.side() + "-query-input").byName("value", dataSetParams.query()).className("query-input");

        HtmlTagBuilder recordsPerPageLabel = queryPane.append().tag().byName("label");
        recordsPerPageLabel.append().text("Records per page:");

        HtmlTagBuilder recordsPerPageInput = queryPane.append().tag().byName("input");
        recordsPerPageInput.attributes().id(dataSetParams.side() + "-records-per-page-input").className("records-per-page").byName("value", String.valueOf(dataSetParams.recordsPerPage()));

        HtmlTagBuilder applyButton = queryPane.append().tag().byName("button");
        applyButton.append().text("Apply");
        //language=JS
        String applyButtonScript = """
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProjectId', document.getElementById('target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, '<SIDE>Query', document.getElementById('<SIDE>-query-input').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, '<SIDE>RecordsPerPage', document.getElementById('<SIDE>-records-per-page-input').value);
                top.location.reload()"""
                .replace("<SIDE>", dataSetParams.side().toString());
        applyButton.attributes().onClick(applyButtonScript);

        HtmlTagBuilder resetButton = queryPane.append().tag().byName("button");
        resetButton.append().text("Reset");
        //language=JS
        String resetButtonScript = """
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProjectId', document.getElementById('target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, '<SIDE>Query', '');
                top.location.href = DiffTool.replaceUrlParam(top.location.href, '<SIDE>RecordsPerPage', '20');
                top.location.reload()"""
                .replace("<SIDE>", dataSetParams.side().toString());
        resetButton.attributes().onClick(resetButtonScript);
    }

    protected void renderTargetProject(@NotNull HtmlTagBuilder parent, @Nullable DataSetWidgetParams dataSetParams) {
        HtmlTagBuilder targetProjectPane = parent.append().tag().div();
        targetProjectPane.attributes().className("target-project");

        targetProjectPane.append().tag().byName("label").append().text("Target project:");
        HtmlTagBuilder targetProjectSelect = targetProjectPane.append().tag().byName("select");
        targetProjectSelect.attributes().id("target-project-selector");

        IProject firstProjectInList = null;
        boolean preDefined = false;

        List<IProject> projects = polarionService.getProjects();
        for (IProject project : projects) {
            if (firstProjectInList == null) {
                firstProjectInList = project;
            }
            HtmlTagBuilder option = targetProjectSelect.append().tag().byName("option");
            option.attributes().byName("value", project.getId());
            option.append().text(project.getName());
            if (dataSetParams != null && project.getId().equals(dataSetParams.projectId())) {
                preDefined = true;
                option.attributes().booleanAttribute("selected");
            }
        }
        if (dataSetParams != null && !preDefined && firstProjectInList != null) {
            dataSetParams.projectId(firstProjectInList.getId());
        }
    }

    protected void renderHeaderRow(@NotNull HtmlContentBuilder parent) {
        renderHeaderRow(parent, false);
    }

    protected void renderHeaderRow(@NotNull HtmlContentBuilder parent, boolean includeSelectAll) {
        HtmlTagBuilder row = parent.tag().tr();
        row.attributes().className("polarion-rpw-table-header-row");
        HtmlTagBuilder th = row.append().tag().th();
        if (includeSelectAll) {
            HtmlTagBuilder checkbox = th.append().tag().byName("input");
            checkbox.attributes().byName("type", "checkbox").className("export-all");
            //language=JS
            checkbox.attributes().onClick("DiffTool.selectAllItems(this);");
        }

        for (Field column : getFields()) {
            row.append().tag().th().append().text(column.label());
        }
    }

    protected void renderContentRows(@NotNull HtmlContentBuilder builder, @NotNull DataSetWidgetParams dataSetParams) {
        if (dataSetParams.items() == null) {
            return;
        }

        List<ModelObject> itemsList = dataSetParams.items().toArrayList();
        int pageEnd = dataSetParams.currentPage() * dataSetParams.recordsPerPage(); // Potential end based on page number and page size
        int pageStart = pageEnd - dataSetParams.recordsPerPage();
        pageEnd = Math.min(pageEnd, itemsList.size()); // Recalculate not to go out of boundaries
        for (int i = pageStart; i < pageEnd; i++) {
            HtmlTagBuilder tr = builder.tag().tr();
            tr.attributes().className("polarion-rpw-table-content-row");
            this.renderItem(tr.append(), itemsList.get(i), dataSetParams);
        }
    }

    protected String getValue(@NotNull ModelObject item, @NotNull String fieldName) {
        Object fieldValue = item.getOldApi().getValue(fieldName);
        return fieldValue == null ? "" : fieldValue.toString();
    }

    protected void renderNotReadableRow(@NotNull HtmlContentBuilder builder, @NotNull String message) {
        HtmlTagBuilder td = builder.tag().td();
        td.attributes().colspan("" + (getFields().size() + 1)).className("polarion-rpw-table-not-readable-cell");
        td.append().text(message);
    }

    protected void renderFooter(@NotNull HtmlTagBuilder parent, @NotNull DataSetWidgetParams dataSetParams) {
        if (dataSetParams.dataSet() != null) {
            (new BottomQueryLinksBuilder(context, dataSetParams.dataSet(), dataSetParams.recordsPerPage())).render(parent);

            if (dataSetParams.lastPage() > 1) {
                renderPaginator(parent, dataSetParams);
            }
        }
    }

    protected void renderPaginator(@NotNull HtmlTagBuilder parent, @NotNull DataSetWidgetParams dataSetParams) {
        HtmlTagBuilder paginator = parent.append().tag().div();
        paginator.attributes().className("paginator");

        if (dataSetParams.currentPage() > 1) {
            renderPaginatorLink(paginator, "<<", 1, dataSetParams.side());
            renderPaginatorLink(paginator, "<", dataSetParams.currentPage() - 1, dataSetParams.side());

            if (dataSetParams.currentPage() - 3 > 0) {
                paginator.append().tag().span().append().text("...");
            }

            int prevPrevPage = dataSetParams.currentPage() - 2;
            if (prevPrevPage > 0) {
                renderPaginatorLink(paginator, String.valueOf(prevPrevPage), prevPrevPage, dataSetParams.side());
            }

            renderPaginatorLink(paginator, String.valueOf(dataSetParams.currentPage() - 1), dataSetParams.currentPage() - 1, dataSetParams.side());
        }

        HtmlTagBuilder currentSpan = paginator.append().tag().span();
        currentSpan.append().text(String.valueOf(dataSetParams.currentPage()));

        if (dataSetParams.currentPage() < dataSetParams.lastPage()) {
            renderPaginatorLink(paginator, String.valueOf(dataSetParams.currentPage() + 1), dataSetParams.currentPage() + 1, dataSetParams.side());

            int nextNextPage = dataSetParams.currentPage() + 2;
            if (nextNextPage <= dataSetParams.lastPage()) {
                renderPaginatorLink(paginator, String.valueOf(nextNextPage), nextNextPage, dataSetParams.side());
            }

            if (dataSetParams.currentPage() + 3 <= dataSetParams.lastPage()) {
                paginator.append().tag().span().append().text("...");
            }

            renderPaginatorLink(paginator, ">", dataSetParams.currentPage() + 1, dataSetParams.side());
            renderPaginatorLink(paginator, ">>", dataSetParams.lastPage(), dataSetParams.side());
        }
    }

    protected void renderPaginatorLink(@NotNull HtmlTagBuilder paginator, @NotNull String text, int page, @NotNull DataSetWidgetParams.Side side) {
        //language=JS
        String paginatorScript = """
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProjectId', document.getElementById('target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, '<SIDE>RecordsPerPage', document.getElementById('<SIDE>-records-per-page-input').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, '<SIDE>Page', '<PAGE_NUMBER>');
                top.location.reload()"""
                .replace("<SIDE>", side.toString())
                .replace("<PAGE_NUMBER>", String.valueOf(page));

        HtmlTagBuilder link = paginator.append().tag().a();
        link.append().text(text);
        link.attributes().onClick(paginatorScript);
    }

    protected abstract void renderItem(@NotNull HtmlContentBuilder builder, @NotNull ModelObject item, @NotNull DataSetWidgetParams dataSetParams);
}

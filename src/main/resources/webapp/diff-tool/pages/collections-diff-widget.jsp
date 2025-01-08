<%@ page import="com.polarion.alm.shared.api.transaction.TransactionalExecutor" %>
<%@ page import="com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction" %>
<%@ page import="java.util.Objects" %>
<%@ page import="com.polarion.portal.internal.shared.navigation.ProjectScope" %>
<%@ page import="com.polarion.alm.shared.api.Scope" %>
<%@ page import="com.polarion.alm.shared.api.impl.ScopeImpl" %>
<%@ page import="com.polarion.alm.server.api.model.rp.widget.impl.RichPageWidgetRenderingContextImpl" %>
<%@ page import="com.polarion.alm.server.api.model.rp.widget.WidgetResourcesServlet" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.widgets.CollectionsDiffWidgetRenderer" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.widgets.DataSetWidgetParams" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.widgets.DiffWidgetParams" %>
<%@ page contentType="text/html; charset=UTF-8" %>

<%
    String sourceProjectId = request.getParameter("sourceProjectId");
    Scope sourceProjectScope = StringUtils.isNotBlank(sourceProjectId) ? new ScopeImpl(new ProjectScope(sourceProjectId)) : null;
    String renderedContent = Objects.requireNonNull(TransactionalExecutor.executeInReadOnlyTransaction(transaction -> {
        RichPageWidgetRenderingContextImpl renderingContext = new RichPageWidgetRenderingContextImpl((InternalReadOnlyTransaction) transaction,
                sourceProjectScope, WidgetResourcesServlet.createResourceUrl("diff-tool"));

        DataSetWidgetParams sourceParams = DataSetWidgetParams.fromRequest(request, DataSetWidgetParams.Side.SOURCE);
        DataSetWidgetParams targetParams = DataSetWidgetParams.fromRequest(request, DataSetWidgetParams.Side.TARGET);

        CollectionsDiffWidgetRenderer renderer = new CollectionsDiffWidgetRenderer(renderingContext,
                DiffWidgetParams.builder()
                        .sourceParams(sourceParams)
                        .targetParams(targetParams)
                        .linkRole(request.getParameter("linkRole"))
                        .configuration(request.getParameter("configuration"))
                        .build());
        return renderer.render();
    }));
    out.println(renderedContent);
%>

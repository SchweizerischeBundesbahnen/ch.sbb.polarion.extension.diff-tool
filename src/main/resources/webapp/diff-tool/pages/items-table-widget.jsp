<%@ page import="com.polarion.alm.shared.api.transaction.TransactionalExecutor" %>
<%@ page import="com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction" %>
<%@ page import="java.util.Objects" %>
<%@ page import="com.polarion.portal.internal.shared.navigation.ProjectScope" %>
<%@ page import="com.polarion.alm.shared.api.Scope" %>
<%@ page import="com.polarion.alm.shared.api.impl.ScopeImpl" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.widgets.WorkItemsDiffWidgetRenderer" %>
<%@ page import="com.polarion.alm.server.api.model.rp.widget.impl.RichPageWidgetRenderingContextImpl" %>
<%@ page import="com.polarion.alm.server.api.model.rp.widget.WidgetResourcesServlet" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.widgets.WorkItemsDiffWidgetParams" %>
<%@ page contentType="text/html; charset=UTF-8" %>

<%
    String projectId = request.getParameter("projectId");
    Scope projectScope = StringUtils.isNotBlank(projectId) ? new ScopeImpl(new ProjectScope(projectId)) : null;
    String renderedContent = Objects.requireNonNull(TransactionalExecutor.executeInReadOnlyTransaction(transaction -> {
        RichPageWidgetRenderingContextImpl renderingContext = new RichPageWidgetRenderingContextImpl((InternalReadOnlyTransaction) transaction,
                projectScope, WidgetResourcesServlet.createResourceUrl("diff-tool"));
        WorkItemsDiffWidgetRenderer renderer = new WorkItemsDiffWidgetRenderer(renderingContext,
                WorkItemsDiffWidgetParams.builder()
                        .query(request.getParameter("query"))
                        .page(request.getParameter("page"))
                        .configuration(request.getParameter("configuration"))
                        .linkRole(request.getParameter("linkRole")).build());
        return renderer.render();
    }));
    out.println(renderedContent);
%>

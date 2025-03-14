package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultipleWorkItemsNode extends NavigationExtenderNode {
    public static final String NODE_ID = "compare-work-items";

    @NotNull
    @Override
    public String getId() {
        return NODE_ID;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Multiple Work Items";
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return "/polarion/icons/default/topicIcons/documentsAndWiki.svg";
    }

    @Nullable
    @Override
    public String getPageUrl(IContextId contextId) {
        String contextName = contextId.getContextName();
        return "/polarion/diff-tool/pages/multiple-work-items.jsp?sourceProjectId=" + (contextName == null || contextName.startsWith("-") ? "" : contextName) + "&buildId=" + System.getProperty("polarion.build.id");
    }

    @Override
    public boolean requiresToken() {
        return false;
    }

    @NotNull
    @Override
    public List<NavigationExtenderNode> getChildren() {
        return List.of();
    }
}

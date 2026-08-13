package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CollectionsNode extends NavigationExtenderNode {
    public static final String NODE_ID = "compare-collections";

    @NotNull
    @Override
    public String getId() {
        return NODE_ID;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Collections";
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return "/polarion/ria/images/topicIconsSmall/collectionsTopic.svg";
    }

    @Nullable
    @Override
    public String getPageUrl(IContextId contextId) {
        return DiffToolNavigationExtender.topicPageUrl(NODE_ID, contextId);
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

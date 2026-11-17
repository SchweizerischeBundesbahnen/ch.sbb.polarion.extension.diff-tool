package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The topic which exports the diffs of several document pairs to PDF at once.
 */
public class DocumentsNode extends NavigationExtenderNode {
    public static final String NODE_ID = "documents-diff-to-pdf";

    @NotNull
    @Override
    public String getId() {
        return NODE_ID;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Documents diff to PDF";
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return "/polarion/ria/images/topicIconsSmall/documentsAndWiki.svg";
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

package ch.sbb.polarion.extension.diff_tool;

import com.polarion.alm.ui.server.navigation.NavigationExtender;
import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DiffToolNavigationExtender extends NavigationExtender {
    public static final String DIFF_TOOL = "diff-tool";

    @NotNull
    @Override
    public String getId() {
        return DIFF_TOOL;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Diff Tool";
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return "/polarion/icons/default/topicIcons/documentsAndWiki.svg";
    }

    @Nullable
    @Override
    public String getPageUrl(@NotNull IContextId contextId) {
        String contextName = contextId.getContextName();
        String scope = contextName == null ? "" : "project/%s/".formatted(contextName);
        return "/polarion/diff-tool-admin/pages/about.jsp?scope=" + scope;
    }

    @Override
    public boolean requiresToken() {
        return false;
    }

    @NotNull
    @Override
    public List<NavigationExtenderNode> getRootNodes(@NotNull IContextId contextId) {
        return new ArrayList<>();
    }
}

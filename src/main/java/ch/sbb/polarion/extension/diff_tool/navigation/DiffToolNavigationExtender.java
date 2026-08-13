package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.alm.ui.server.navigation.NavigationExtender;
import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DiffToolNavigationExtender extends NavigationExtender {
    public static final String DIFF_TOOL = "diff-tool";

    /**
     * The React page behind every Diff Tool topic. Which topic it renders comes from {@code ?topic=}, whose
     * values are the node ids below - the same arrangement as the admin pages and their {@code ?feature=}.
     */
    static final String TOPICS_PAGE_URL = "/polarion/diff-tool-app/ui/app/topics.html";

    /**
     * {@code sourceProjectId} stays empty outside a project scope, where the context name is either null or a
     * project group name, which Polarion prefixes with a dash.
     */
    @NotNull
    static String topicPageUrl(@NotNull String topicId, @Nullable IContextId contextId) {
        String contextName = contextId == null ? null : contextId.getContextName();
        String projectId = contextName == null || contextName.startsWith("-") ? "" : contextName;
        return "%s?topic=%s&sourceProjectId=%s&buildId=%s".formatted(TOPICS_PAGE_URL, topicId, projectId, System.getProperty("polarion.build.id"));
    }

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
        return "/polarion/diff-tool-app/ui/images/menu/30x30/_parent.svg";
    }

    @Nullable
    @Override
    public String getPageUrl(@NotNull IContextId contextId) {
        return topicPageUrl(DIFF_TOOL, contextId);
    }

    @Override
    public boolean requiresToken() {
        return false;
    }

    @NotNull
    @Override
    public List<NavigationExtenderNode> getRootNodes(@NotNull IContextId contextId) {
        return List.of(new MultipleWorkItemsNode(), new CollectionsNode());
    }
}

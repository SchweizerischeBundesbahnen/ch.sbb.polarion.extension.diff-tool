package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A topic below the Diff Tool node of the navigation. Each topic is a page of {@code topics.html}, chosen by its
 * node ID, and has no topics of its own.
 */
public abstract class DiffToolTopicNode extends NavigationExtenderNode {
    private final String id;
    private final String label;
    private final String iconUrl;

    protected DiffToolTopicNode(@NotNull String id, @NotNull String label, @NotNull String iconUrl) {
        this.id = id;
        this.label = label;
        this.iconUrl = iconUrl;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getLabel() {
        return label;
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return iconUrl;
    }

    @Nullable
    @Override
    public String getPageUrl(IContextId contextId) {
        return DiffToolNavigationExtender.topicPageUrl(id, contextId);
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

package ch.sbb.polarion.extension.diff_tool.service.handler;

import ch.sbb.polarion.extension.diff_tool.service.handler.impl.EnumReplaceHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.HyperlinksHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.IdsHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.ImageHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.LinksHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.OuterWrapperHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.ReverseStylesHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.LinkedWorkItemsHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DiffLifecycleHandler {

    /*
     * Link must have specific class attribute, data-type="workItem" and data-item-id which contains work item ID.
     * Also link may have data-scope & data-revision attributes in some cases so in this regex they are optional.
     * Note that all those attributes may come in random order.
     */
    String LINK_REGEX = "<span\\s+" +
            "(?=[^>]*class=\"polarion-rte-link\")" +
            "(?=[^>]*data-type=\"workItem\")" +
            "(?=[^>]*data-item-id=\"(?<workItemId>[^\"]+?)\")" +
            "(?=[^>]*data-scope=\"(?<workItemProjectId>[^\"]+)\")?" +
            "(?=[^>]*data-revision=\"(?<workItemRevision>[^\"]+)\")?" +
            "[^>]*?>";

    default @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        return initialPair;
    }

    default @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        return diff;
    }

    // Note that the order may be important:
    // preProcess calls will be called using direct order in this list whereas postProcess - using reversed order.
    static List<DiffLifecycleHandler> getHandlers() {
        return List.of(
                new IdsHandler(),
                new LinkedWorkItemsHandler(),
                new LinksHandler(),
                new HyperlinksHandler(),
                new ImageHandler(),
                new OuterWrapperHandler(),
                new EnumReplaceHandler(),
                new ReverseStylesHandler()
        );
    }
}

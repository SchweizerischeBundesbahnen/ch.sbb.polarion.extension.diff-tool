package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.LinkRole;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import ch.sbb.polarion.extension.diff_tool.util.ModificationType;
import com.polarion.alm.tracker.internal.model.HyperlinkStruct;
import com.polarion.platform.persistence.IEnumOption;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.sbb.polarion.extension.diff_tool.util.ModificationType.*;
import static com.polarion.alm.tracker.model.IWorkItem.KEY_HYPERLINKS;

/**
 * Provides custom diff logic for 'hyperlinks' field.
 */
@NoArgsConstructor
public class HyperlinksHandler implements DiffLifecycleHandler {

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        // skip html comparison just to save resources, we do not need it as we'll render the result by ourselves later
        return isInappropriateCaseForHandler(context) ? initialPair : Pair.of("", "");
    }

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        if (isInappropriateCaseForHandler(context)) {
            return diff;
        }

        String wiTypeId = Optional.ofNullable(ObjectUtils.firstNonNull(context.getWorkItemA(), context.getWorkItemB())
                .getUnderlyingObject().getType()).map(IEnumOption::getId).orElse(null);
        if (wiTypeId == null) {
            return diff;
        }

        List<String> resultRows = new ArrayList<>();
        List<String> rolesToMerge = Optional.of(context.getDiffModel().getHyperlinkRoles())
                .filter(settingsRoles -> !settingsRoles.isEmpty())
                .orElseGet(() -> context.getPolarionService().getHyperlinkRoles(context.getLeftProjectId()).stream().map(LinkRole::getCombinedId).toList())
                .stream().filter(combinedId -> combinedId.startsWith(wiTypeId + "#"))
                .map(combinedId -> combinedId.substring(combinedId.indexOf("#") + 1)).toList();

        List<HyperlinkStruct> allLinksA = getHyperlinks(context.getWorkItemA());
        List<HyperlinkStruct> allLinksB = getHyperlinks(context.getWorkItemB());

        // first we find same links & display them on top of the diff
        for (String roleId : rolesToMerge) {
            List<HyperlinkStruct> linksA = filterByRole(allLinksA, roleId);
            List<HyperlinkStruct> linksB = filterByRole(allLinksB, roleId);
            for (HyperlinkStruct link : linksA) {
                HyperlinkStruct oppositeLink = linksB.stream().filter(l -> l.getUri().equals(link.getUri())).findFirst().orElse(null);
                if (oppositeLink != null) {
                    allLinksA.remove(link);
                    allLinksB.remove(oppositeLink);
                    linksB.remove(oppositeLink);
                    resultRows.add(markChanged(renderLink(link), NONE));
                }
            }
        }

        // the rest are links only one work item has
        processOneSidedLinks(resultRows, rolesToMerge, allLinksA, allLinksB, context.reverseStyles());

        return String.join("", resultRows);
    }

    /**
     * Renders the links only one work item has: removed if A has it, added if B has it. So each of the two diffs
     * holds the whole change: the unified view shows one of them, and side by side each leaf hides the other half.
     * ReverseStylesHandler post-processes before this handler and never sees these rows, so the reversed diff
     * swaps them here. Removed rows come first, as in a text diff.
     */
    private void processOneSidedLinks(List<String> resultRows, List<String> rolesToMerge,
                                      List<HyperlinkStruct> linksA, List<HyperlinkStruct> linksB, boolean reverseStyles) {
        for (String roleId : rolesToMerge) {
            List<String> rowsA = renderOneSidedLinks(linksA, roleId, reverseStyles ? ADDED : REMOVED);
            List<String> rowsB = renderOneSidedLinks(linksB, roleId, reverseStyles ? REMOVED : ADDED);
            resultRows.addAll(reverseStyles ? rowsB : rowsA);
            resultRows.addAll(reverseStyles ? rowsA : rowsB);
        }
    }

    private List<String> renderOneSidedLinks(List<HyperlinkStruct> links, String roleId, ModificationType modificationType) {
        return filterByRole(links, roleId).stream()
                .map(link -> markChanged(renderLink(link), modificationType))
                .toList();
    }

    private List<HyperlinkStruct> getHyperlinks(WorkItem workItem) {
        return workItem == null ? List.of() : ((Collection<?>) workItem.getUnderlyingObject().getHyperlinks()).stream()
                .filter(HyperlinkStruct.class::isInstance)
                .map(HyperlinkStruct.class::cast)
                .sorted(Comparator.comparing(HyperlinkStruct::getUri))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<HyperlinkStruct> filterByRole(List<HyperlinkStruct> links, String roleId) {
        return links.stream()
                .filter(s -> s.getRole() != null && Objects.equals(roleId, s.getRole().getId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String renderLink(HyperlinkStruct link) {
        return """
                <span class="polarion-JSEnumOption" title="external reference">%1$s</span>:
                <a href="%2$s" target="_blank" class="polarion-Hyperlink">
                    <span class="polarion-JSTextRenderer-Text" title="%2$s">%2$s</span>
                </a>""".formatted(link.getRole().getName(), link.getUri());
    }

    private String markChanged(String text, @NotNull ModificationType modificationType) {
        return "<div class=\"diff-hl-container\"><div class=\"%s diff-hl\">%s</div></div>".formatted(modificationType.getStyleName(), text);
    }

    private boolean isInappropriateCaseForHandler(DiffContext context) {
        return !Stream.of(context.getFieldA().getId(), context.getFieldB().getId()).filter(Objects::nonNull).distinct().toList().equals(List.of(KEY_HYPERLINKS));
    }
}

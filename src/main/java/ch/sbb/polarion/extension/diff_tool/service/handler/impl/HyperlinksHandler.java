package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.HyperlinkRole;
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

        String wiTypeId = Optional.ofNullable(ObjectUtils.firstNonNull(context.workItemA, context.workItemB)
                .getUnderlyingObject().getType()).map(IEnumOption::getId).orElse(null);
        if (wiTypeId == null) {
            return diff;
        }

        List<String> resultRows = new ArrayList<>();
        List<String> rolesToMerge = Optional.of(context.diffModel.getHyperlinkRoles())
                .filter(settingsRoles -> !settingsRoles.isEmpty())
                .orElseGet(() -> context.polarionService.getHyperlinkRoles(context.leftProjectId).stream().map(HyperlinkRole::getCombinedId).toList())
                .stream().filter(combinedId -> combinedId.startsWith(wiTypeId + "#"))
                .map(combinedId -> combinedId.substring(combinedId.indexOf("#") + 1)).toList();

        List<HyperlinkStruct> allLinksA = getHyperlinks(context.workItemA);
        List<HyperlinkStruct> allLinksB = getHyperlinks(context.workItemB);

        // first we find same links & display them on top of the diff
        for (String roleId : rolesToMerge) {
            List<HyperlinkStruct> linksA = filterByRole(allLinksA, roleId);
            List<HyperlinkStruct> linksB = filterByRole(allLinksB, roleId);
            for (HyperlinkStruct link : linksA) {
                HyperlinkStruct oppositeLink = linksB.stream().filter(l -> l.getUri().equals(link.getUri())).findFirst().orElse(null);
                if (oppositeLink != null) {
                    allLinksB.remove(oppositeLink);
                    linksB.remove(oppositeLink);
                    resultRows.add(markChanged(renderLink(link), NONE));
                }
            }
        }

        // the rest items go after
        for (String roleId : rolesToMerge) {
            List<HyperlinkStruct> linksB = filterByRole(allLinksB, roleId);
            linksB.forEach(link -> {
                resultRows.add(markChanged(renderLink(link), ADDED));
                resultRows.add(markChanged(renderLink(link), REMOVED));
            });
        }

        return String.join("", resultRows);
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
        return !Stream.of(context.fieldA.getId(), context.fieldB.getId()).filter(Objects::nonNull).distinct().toList().equals(List.of(KEY_HYPERLINKS));
    }
}

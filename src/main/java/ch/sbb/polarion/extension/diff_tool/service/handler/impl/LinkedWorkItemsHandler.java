package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import com.polarion.alm.server.ServerUiContext;
import com.polarion.alm.server.api.model.wi.linked.LinkedWorkItemRendererImpl;
import com.polarion.alm.server.api.model.wi.linked.ProxyLinkedWorkItem;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.sbb.polarion.extension.diff_tool.service.handler.impl.LinkedWorkItemsHandler.ModificationType.*;
import static com.polarion.alm.tracker.model.IWorkItem.KEY_LINKED_WORK_ITEMS;

/**
 * Provides custom diff logic for 'linkedWorkItems' field.
 */
@NoArgsConstructor
public class LinkedWorkItemsHandler implements DiffLifecycleHandler {

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        // skip html comparison just to save resources, we do not need it as we'll render the result by ourselves later
        return isInappropriateCaseForHandler(context) ? initialPair : Pair.of("", "");
    }

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        if (isInappropriateCaseForHandler(context) || context.workItemA == null || context.workItemB == null) {
            return diff;
        }

        List<String> resultRows = new ArrayList<>();

        IWorkItem workItemA = context.workItemA.getUnderlyingObject();
        IWorkItem workItemB = context.workItemB.getUnderlyingObject();

        List<ILinkedWorkItemStruct> linksA = sortLinks(workItemA.getLinkedWorkItemsStructsDirect());
        List<ILinkedWorkItemStruct> linksABack = sortLinks(workItemA.getLinkedWorkItemsStructsBack());
        List<ILinkedWorkItemStruct> linksB = sortLinks(workItemB.getLinkedWorkItemsStructsDirect());
        List<ILinkedWorkItemStruct> linksBBack = sortLinks(workItemB.getLinkedWorkItemsStructsBack());
        List<ILinkRoleOpt> allRoles = Stream.concat(Stream.concat(linksA.stream(), linksABack.stream()), Stream.concat(linksB.stream(), linksBBack.stream())).map(ILinkedWorkItemStruct::getLinkRole).distinct().sorted(Comparator.comparing(IEnumOption::getName)).toList();

        List<ILinkedWorkItemStruct> toRemove = new ArrayList<>();

        // first, we attempt to find all items that interlinked or have links to the same work item
        allRoles.forEach(linkRole -> {
            linksB.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(link -> {
                if (sameWorkItem(link, workItemA)) {
                    resultRows.add(markChanged(renderLinkedItem(link, workItemB, false), NONE));
                    toRemove.add(link);
                }
            });
            linksBBack.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(link -> {
                if (sameWorkItem(link, workItemA)) {
                    resultRows.add(markChanged(renderLinkedItem(link, workItemB, true), NONE));
                    toRemove.add(link);
                }
            });

            linksA.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(linkA -> linksB.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(linkB -> {
                if (sameWorkItem(linkA, linkB.getLinkedItem())) {
                    resultRows.add(markChanged(renderLinkedItem(linkB, workItemB, false), Objects.equals(linkA.getRevision(), linkB.getRevision()) ? NONE : MODIFIED));
                    toRemove.add(linkA);
                    toRemove.add(linkB);
                }
            }));
        });
        linksA.removeAll(toRemove);
        linksB.removeAll(toRemove);
        toRemove.clear();

        // next we mark as the same counterparts (items which are linked to the interlinked work items)
        allRoles.forEach(linkRole -> linksB.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(link -> {
            Set<String> oppositeWorkItems = linksA.stream()
                    .filter(l -> !belongsToModule(l.getLinkedItem(), workItemB.getModule())) // filter out direct links to work item B
                    .map(l -> l.getLinkedItem().getId()).collect(Collectors.toSet());
            if (sameProjectItems(workItemB, link.getLinkedItem())) {
                List<IWorkItem> pairedWorkItems = context.polarionService.getPairedWorkItems(link.getLinkedItem(), workItemA.getProjectId(), context.pairedWorkItemsLinkRole);
                if (pairedWorkItems.stream().anyMatch(w -> oppositeWorkItems.contains(w.getId()))) {
                    resultRows.add(markChanged(renderLinkedItem(link, workItemB, false), NONE));
                    toRemove.add(link);
                }
            }
        }));
        linksB.removeAll(toRemove);

        // rest items we mark as added/removed
        allRoles.forEach(linkRole -> linksB.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(link -> {
            resultRows.add(markChanged(renderLinkedItem(link, workItemB, false), ADDED));
            resultRows.add(markChanged(renderLinkedItem(link, workItemB, false), REMOVED));
        }));
        return String.join("", resultRows);
    }

    private String renderLinkedItem(ILinkedWorkItemStruct link, IWorkItem parentWorkItem, boolean backLink) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(trx -> {
            HtmlFragmentBuilder fragmentBuilder = RichTextRenderTarget.EDITOR.selectBuilderTarget(trx.context().createHtmlFragmentBuilderFor());
            new LinkedWorkItemRendererImpl(ServerUiContext.getInstance(), new ProxyLinkedWorkItem(link, parentWorkItem, (InternalReadOnlyTransaction) trx, backLink)).withLinksToDocument().htmlTo(fragmentBuilder);
            return fragmentBuilder.toString().replace("white-space: normal", ""); // remove unwanted style
        });
    }

    private boolean belongsToModule(IWorkItem linkedItem, IModule module) {
        return Objects.equals(linkedItem.getProjectId(), module.getProjectId()) &&
                linkedItem.getModule() != null && // work item may exist outside any document
                linkedItem.getModule().getModuleLocation().removeRevision().equals(module.getModuleLocation().removeRevision());
    }

    private String markChanged(String text, @NotNull ModificationType modificationType) {
        return "<div class=\"diff-lwi-container\"><div class=\"%s diff-lwi\">%s</div></div>".formatted(modificationType.styleName, text);
    }

    private List<ILinkedWorkItemStruct> sortLinks(Collection<ILinkedWorkItemStruct> links) {
        return new ArrayList<>(links.stream().sorted(Comparator.comparing(o -> o.getLinkedItem().getId())).toList());
    }

    private boolean sameWorkItem(ILinkedWorkItemStruct linkA, IWorkItem itemB) {
        return sameProjectItems(linkA.getLinkedItem(), itemB) && Objects.equals(linkA.getLinkedItem().getId(), itemB.getId());
    }

    private boolean sameProjectItems(IWorkItem itemA, IWorkItem itemB) {
        return Objects.equals(itemA.getProjectId(), itemB.getProjectId());
    }

    private boolean isInappropriateCaseForHandler(DiffContext context) {
        return context.pairedWorkItemsDiffer || !Objects.equals(context.fieldA.getId(), KEY_LINKED_WORK_ITEMS) || !Objects.equals(context.fieldB.getId(), KEY_LINKED_WORK_ITEMS);
    }

    enum ModificationType {
        ADDED("diff-html-added"),
        REMOVED("diff-html-removed"),
        MODIFIED("diff-html-changed"),
        NONE("unchanged");

        final String styleName;

        ModificationType(String styleName) {
            this.styleName = styleName;
        }
    }
}

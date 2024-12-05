package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import ch.sbb.polarion.extension.diff_tool.util.ModificationType;
import com.polarion.alm.server.ServerUiContext;
import com.polarion.alm.server.api.model.wi.linked.LinkedWorkItemRendererImpl;
import com.polarion.alm.server.api.model.wi.linked.ProxyLinkedWorkItem;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils.*;
import static ch.sbb.polarion.extension.diff_tool.util.ModificationType.*;
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
        if (isInappropriateCaseForHandler(context)) {
            return diff;
        }

        List<String> resultRows = new ArrayList<>();

        IWorkItem workItemA = context.workItemA == null ? null : context.workItemA.getUnderlyingObject();
        List<ILinkedWorkItemStruct> linksA = getLinks(workItemA, false);
        List<ILinkedWorkItemStruct> linksABack = getLinks(workItemA, true);

        IWorkItem workItemB = context.workItemB == null ? null : context.workItemB.getUnderlyingObject();
        List<ILinkedWorkItemStruct> linksB = getLinks(workItemB, false);
        List<ILinkedWorkItemStruct> linksBBack = getLinks(workItemB, true);

        List<ILinkRoleOpt> allRoles = Stream.concat(Stream.concat(linksA.stream(), linksABack.stream()), Stream.concat(linksB.stream(), linksBBack.stream()))
                .map(ILinkedWorkItemStruct::getLinkRole).distinct().sorted(Comparator.comparing(IEnumOption::getName)).toList();

        if (workItemA != null && workItemB != null) {
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

                linksA.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(linkA ->
                        linksB.stream().filter(l -> l.getLinkRole().equals(linkRole)).forEach(linkB -> {
                            if (sameWorkItem(linkA, linkB.getLinkedItem())) {
                                resultRows.add(markChanged(renderLinkedItem(linkB, workItemB, false),
                                        // show links as same when it's a link to some 3rd project
                                        !sameProjectItems(workItemA, workItemB) && !sameProjectItems(workItemA, linkB.getLinkedItem()) && !sameProjectItems(workItemB, linkB.getLinkedItem()) ||
                                        // or we compare work items from same project and link leads to the same revision
                                        Objects.equals(linkA.getRevision(), linkB.getRevision()) && sameProjectItems(workItemA, workItemB) ? NONE : MODIFIED));
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
                        .filter(l -> notFromModule(l.getLinkedItem(), workItemB.getModule())) // filter out direct links to work item B
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
        }

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

    private String markChanged(String text, @NotNull ModificationType modificationType) {
        return "<div class=\"diff-lwi-container\"><div class=\"%s diff-lwi\">%s</div></div>".formatted(modificationType.getStyleName(), text);
    }

    private boolean isInappropriateCaseForHandler(DiffContext context) {
        return context.pairedWorkItemsDiffer || !Stream.of(context.fieldA.getId(), context.fieldB.getId()).filter(Objects::nonNull).distinct().toList().equals(List.of(KEY_LINKED_WORK_ITEMS));
    }
}

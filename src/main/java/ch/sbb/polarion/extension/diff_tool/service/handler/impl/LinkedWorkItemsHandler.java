package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import ch.sbb.polarion.extension.diff_tool.util.ModificationType;
import com.polarion.alm.server.api.model.wi.linked.LinkedWorkItemRendererImpl;
import com.polarion.alm.server.api.model.wi.linked.ProxyLinkedWorkItem;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.IEnumOption;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

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
    private static final Logger logger = Logger.getLogger(LinkedWorkItemsHandler.class);

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

        IWorkItem workItemA = context.getWorkItemA() == null ? null : context.getWorkItemA().getUnderlyingObject();
        List<ILinkedWorkItemStruct> linksA = getLinks(workItemA, false);
        List<ILinkedWorkItemStruct> linksABack = getLinks(workItemA, true);

        IWorkItem workItemB = context.getWorkItemB() == null ? null : context.getWorkItemB().getUnderlyingObject();
        List<ILinkedWorkItemStruct> linksB = getLinks(workItemB, false);
        List<ILinkedWorkItemStruct> linksBBack = getLinks(workItemB, true);

        List<ILinkRoleOpt> allRoles = collectAllRoles(context, linksA, linksABack, linksB, linksBBack);

        if (workItemA != null && workItemB != null) {
            processInterlinkedItems(resultRows, allRoles, linksA, linksB, linksBBack, workItemA, workItemB);
            processCounterparts(resultRows, allRoles, linksA, linksB, workItemA, workItemB, context);
        }

        // rest items we mark as added/removed
        processRemainingItems(resultRows, allRoles, linksB, workItemB);
        return String.join("", resultRows);
    }

    private List<ILinkRoleOpt> collectAllRoles(DiffContext context, List<ILinkedWorkItemStruct> linksA,
                                               List<ILinkedWorkItemStruct> linksABack, List<ILinkedWorkItemStruct> linksB,
                                               List<ILinkedWorkItemStruct> linksBBack) {
        return Stream.concat(Stream.concat(linksA.stream(), linksABack.stream()), Stream.concat(linksB.stream(), linksBBack.stream()))
                .map(ILinkedWorkItemStruct::getLinkRole)
                .filter(role -> filterRole(role, context))
                .distinct()
                .sorted(Comparator.comparing(IEnumOption::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private boolean filterRole(ILinkRoleOpt role, DiffContext context) {
        if (role.getName() == null) {
            logger.warn("Link role '%s' with null name encountered: looks like the link role has been removed from configuration".formatted(role.getId()));
        }
        return context.getDiffModel().getLinkedWorkItemRoles().isEmpty() || context.getDiffModel().getLinkedWorkItemRoles().contains(role.getId());
    }

    private void processInterlinkedItems(List<String> resultRows, List<ILinkRoleOpt> allRoles,
                                         List<ILinkedWorkItemStruct> linksA, List<ILinkedWorkItemStruct> linksB,
                                         List<ILinkedWorkItemStruct> linksBBack, IWorkItem workItemA, IWorkItem workItemB) {
        List<ILinkedWorkItemStruct> toRemove = new ArrayList<>();

        for (ILinkRoleOpt linkRole : allRoles) {
            processDirectLinks(resultRows, toRemove, linksB, linkRole, workItemA, workItemB, false);
            processDirectLinks(resultRows, toRemove, linksBBack, linkRole, workItemA, workItemB, true);
            processMatchingLinks(resultRows, toRemove, linksA, linksB, linkRole, workItemA, workItemB);
        }

        linksA.removeAll(toRemove);
        linksB.removeAll(toRemove);
    }

    private void processDirectLinks(List<String> resultRows, List<ILinkedWorkItemStruct> toRemove,
                                    List<ILinkedWorkItemStruct> links, ILinkRoleOpt linkRole,
                                    IWorkItem workItemA, IWorkItem workItemB, boolean backLink) {
        for (ILinkedWorkItemStruct link : links) {
            if (link.getLinkRole().equals(linkRole) && sameWorkItem(link, workItemA)) {
                resultRows.add(markChanged(renderLinkedItem(link, workItemB, backLink), NONE));
                toRemove.add(link);
            }
        }
    }

    private void processMatchingLinks(List<String> resultRows, List<ILinkedWorkItemStruct> toRemove,
                                      List<ILinkedWorkItemStruct> linksA, List<ILinkedWorkItemStruct> linksB,
                                      ILinkRoleOpt linkRole, IWorkItem workItemA, IWorkItem workItemB) {
        for (ILinkedWorkItemStruct linkA : linksA) {
            if (!linkA.getLinkRole().equals(linkRole)) {
                continue;
            }
            for (ILinkedWorkItemStruct linkB : linksB) {
                if (linkB.getLinkRole().equals(linkRole) && sameWorkItem(linkA, linkB.getLinkedItem())) {
                    ModificationType modType = determineModificationType(linkA, linkB, workItemA, workItemB);
                    resultRows.add(markChanged(renderLinkedItem(linkB, workItemB, false), modType));
                    toRemove.add(linkA);
                    toRemove.add(linkB);
                }
            }
        }
    }

    private ModificationType determineModificationType(ILinkedWorkItemStruct linkA, ILinkedWorkItemStruct linkB,
                                                       IWorkItem workItemA, IWorkItem workItemB) {
        // show links as same when it's a link to some 3rd project
        boolean isThirdProjectLink = !sameProjectItems(workItemA, workItemB)
                && !sameProjectItems(workItemA, linkB.getLinkedItem())
                && !sameProjectItems(workItemB, linkB.getLinkedItem());
        // or we compare work items from same project and link leads to the same revision
        boolean isSameRevision = Objects.equals(linkA.getRevision(), linkB.getRevision()) && sameProjectItems(workItemA, workItemB);
        return isThirdProjectLink || isSameRevision ? NONE : MODIFIED;
    }

    private void processCounterparts(List<String> resultRows, List<ILinkRoleOpt> allRoles,
                                     List<ILinkedWorkItemStruct> linksA, List<ILinkedWorkItemStruct> linksB,
                                     IWorkItem workItemA, IWorkItem workItemB, DiffContext context) {
        List<ILinkedWorkItemStruct> toRemove = new ArrayList<>();
        Set<String> oppositeWorkItems = linksA.stream()
                .filter(l -> notFromModule(l.getLinkedItem(), workItemB.getModule()))
                .map(l -> l.getLinkedItem().getId())
                .collect(Collectors.toSet());

        for (ILinkRoleOpt linkRole : allRoles) {
            for (ILinkedWorkItemStruct link : linksB) {
                if (!link.getLinkRole().equals(linkRole) || !sameProjectItems(workItemB, link.getLinkedItem())) {
                    continue;
                }
                List<IWorkItem> pairedWorkItems = context.getPolarionService().getPairedWorkItems(
                        link.getLinkedItem(), workItemA.getProjectId(), context.getPairedWorkItemsLinkRole());
                if (pairedWorkItems.stream().anyMatch(w -> oppositeWorkItems.contains(w.getId()))) {
                    resultRows.add(markChanged(renderLinkedItem(link, workItemB, false), NONE));
                    toRemove.add(link);
                }
            }
        }
        linksB.removeAll(toRemove);
    }

    private void processRemainingItems(List<String> resultRows, List<ILinkRoleOpt> allRoles,
                                       List<ILinkedWorkItemStruct> linksB, IWorkItem workItemB) {
        for (ILinkRoleOpt linkRole : allRoles) {
            for (ILinkedWorkItemStruct link : linksB) {
                if (link.getLinkRole().equals(linkRole)) {
                    resultRows.add(markChanged(renderLinkedItem(link, workItemB, false), ADDED));
                    resultRows.add(markChanged(renderLinkedItem(link, workItemB, false), REMOVED));
                }
            }
        }
    }

    @VisibleForTesting
    String renderLinkedItem(ILinkedWorkItemStruct link, IWorkItem parentWorkItem, boolean backLink) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(trx -> {
            HtmlFragmentBuilder fragmentBuilder = RichTextRenderTarget.EDITOR.selectBuilderTarget(trx.context().createHtmlFragmentBuilderFor());
            InternalReadOnlyTransaction internalReadOnlyTransaction = (InternalReadOnlyTransaction) trx;
            ProxyLinkedWorkItem proxyLinkedWorkItem = new ProxyLinkedWorkItem(link, parentWorkItem, internalReadOnlyTransaction, backLink);
            new LinkedWorkItemRendererImpl(proxyLinkedWorkItem, internalReadOnlyTransaction).withLinksToDocument().htmlTo(fragmentBuilder);
            return fragmentBuilder.toString().replace("white-space: normal", ""); // remove unwanted style
        });
    }

    @VisibleForTesting
    String markChanged(String text, @NotNull ModificationType modificationType) {
        return "<div class=\"diff-lwi-container\"><div class=\"%s diff-lwi\">%s</div></div>".formatted(modificationType.getStyleName(), text);
    }

    @VisibleForTesting
    boolean isInappropriateCaseForHandler(DiffContext context) {
        return context.isPairedWorkItemsDiffer() || !Stream.of(context.getFieldA().getId(), context.getFieldB().getId()).filter(Objects::nonNull).distinct().toList().equals(List.of(KEY_LINKED_WORK_ITEMS));
    }
}

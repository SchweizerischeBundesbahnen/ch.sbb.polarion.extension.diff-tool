package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.util.OutlineNumberComparator;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CalculatePairsContext {

    @Getter
    private final OutlineNumberComparator outlineNumberComparator = new OutlineNumberComparator();
    @Getter
    private final IModule leftDocument;
    @Getter
    private final IModule rightDocument;
    @Getter
    private final List<IWorkItem> leftDocumentWorkItems;
    @Getter
    private final List<IWorkItem> rightDocumentWorkItems;
    private final ILinkRoleOpt linkedByRole;
    private final List<String> statusesToIgnore;
    private final Map<IWorkItem, String> outlineNumbersCache = new HashMap<>();

    public CalculatePairsContext(@NotNull IModule leftDocument, @NotNull IModule rightDocument, @NotNull ILinkRoleOpt linkedByRole, @NotNull List<String> statusesToIgnore) {
        this.leftDocument = leftDocument;
        this.rightDocument = rightDocument;
        this.linkedByRole = linkedByRole;
        this.statusesToIgnore = statusesToIgnore;
        this.leftDocumentWorkItems = leftDocument.getAllWorkItems();
        this.rightDocumentWorkItems = rightDocument.getAllWorkItems();
    }

    public synchronized String getOutlineNumber(IWorkItem workItem, boolean leftDocumentScope) {
        return outlineNumbersCache.computeIfAbsent(workItem, (leftDocumentScope ? leftDocument : rightDocument)::getOutlineNumberOfWorkitem);
    }

    public boolean isReferenced(IWorkItem workItem, boolean leftDocumentScope) {
        return (leftDocumentScope ? leftDocument : rightDocument).getExternalWorkItems().contains(workItem);
    }

    public boolean isSuitableLinkRole(ILinkRoleOpt roleOpt) {
        return Objects.equals(linkedByRole.getId(), roleOpt.getId());
    }

    public boolean hasStatusToIgnore(IWorkItem workItem) {
        return statusesToIgnore.contains(Optional.ofNullable(workItem.getStatus()).map(IEnumOption::getId).orElse(null));
    }

}

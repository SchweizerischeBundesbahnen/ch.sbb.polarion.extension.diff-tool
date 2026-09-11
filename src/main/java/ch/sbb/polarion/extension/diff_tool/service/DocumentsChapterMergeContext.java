package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.ChapterMergePayload;
import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.report.MergeReportEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.LinkRoleDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ReferencedItemsHandling;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Context of a chapter merge, i.e. of copying or moving a part of one document into another one.
 * <p>
 * Its link role is empty on purpose. A copy must not be linked to its origin, so there is no role to seek
 * counterparts by: {@link PolarionService#getPairedWorkItems(IWorkItem, String, String)} finds nothing for
 * an empty role, and counterparts inside the copied chapter are resolved by {@link #getItemMapping()} instead.
 * <p>
 * It is not an {@link IPreserveCommentsContext}: that keeps the comments a merge would otherwise overwrite in the
 * target work item, and the work items a chapter merge writes into are new ones which have no comments yet.
 */
@Getter
public final class DocumentsChapterMergeContext extends SettingsAwareMergeContext
        implements ICopyModuleAttachmentsContext, IUnpairedCopyContext {

    private final DocumentIdentifier sourceDocumentIdentifier;
    private final DocumentIdentifier targetDocumentIdentifier;
    private final IModule sourceModule;
    private final IModule targetModule;
    private final ChapterMergeMode mode;
    private final ChapterInsertMode insertMode;
    private final ReferencedItemsHandling referencedItems;
    private final boolean copyWorkItemLayouts;
    private final String sourceChapterOutlineNumber;
    private final String targetChapterOutlineNumber;

    /** Home page content of the source document as it was before the merge started. In move mode the merge modifies it. */
    private final String sourceHomePageContentSnapshot;

    @Getter(AccessLevel.NONE)
    private final Map<String, IWorkItem> itemMapping = new LinkedHashMap<>();
    private final Map<String, String> idMapping = new LinkedHashMap<>();
    private final List<IWorkItem> createdItems = new ArrayList<>();
    private final List<IWorkItem> movedItems = new ArrayList<>();
    private final List<IWorkItem> referencedItemsInTarget = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final List<String> copiedLayoutTypeIds = new ArrayList<>();
    private final List<IWorkItem> detachCandidates = new ArrayList<>();

    /**
     * Fields to be merged into the work item which is being copied right now. All writable fields of a work item
     * are copied, and which fields these are depends on the item's type, so the model is replaced per item.
     */
    @Setter
    private DiffModel currentDiffModel;

    /** Outline number the merged chapter got in the target document. */
    @Setter
    private String insertedOutlineNumber;

    /**
     * Comments of the work item which is being copied right now, mapped to their copies. Like the fields to be
     * copied, this belongs to one work item at a time.
     */
    @Setter
    @Getter(AccessLevel.NONE)
    private Map<String, String> commentIdMapping = Map.of();

    public DocumentsChapterMergeContext(@NotNull PolarionService polarionService, @NotNull ChapterMergeParams params) {
        super(MergeDirection.LEFT_TO_RIGHT, "", LinkRoleDirection.DIRECT, DiffModel.builder().build(), true);
        this.sourceDocumentIdentifier = params.getSourceDocument();
        this.targetDocumentIdentifier = params.getTargetDocument();
        this.sourceModule = polarionService.getModule(sourceDocumentIdentifier);
        this.targetModule = polarionService.getModule(targetDocumentIdentifier);
        this.mode = params.getMode();
        this.insertMode = params.getInsertMode();
        this.referencedItems = params.getReferencedItems();
        this.copyWorkItemLayouts = params.isCopyWorkItemLayouts();
        this.sourceChapterOutlineNumber = params.getSourceChapterOutlineNumber();
        this.targetChapterOutlineNumber = params.getTargetChapterOutlineNumber();
        this.sourceHomePageContentSnapshot = sourceModule.getHomePageContent() == null ? "" : sourceModule.getHomePageContent().getContent();
    }

    /**
     * A copied work item whose text refers to a document attachment needs that attachment in the target document,
     * so the attachment goes along with it. There is no reason to leave it behind, hence no option for it.
     */
    @Override
    public boolean isCopyMissingDocumentAttachments() {
        return true;
    }

    @Override
    public DiffModel getDiffModel() {
        return currentDiffModel != null ? currentDiffModel : super.getDiffModel();
    }

    public boolean sameProject() {
        return sourceModule.getProjectId().equals(targetModule.getProjectId());
    }

    public void bindCopy(@NotNull IWorkItem sourceWorkItem, @NotNull IWorkItem targetWorkItem) {
        itemMapping.put(sourceWorkItem.getId(), targetWorkItem);
        idMapping.put(sourceWorkItem.getId(), targetWorkItem.getId());
    }

    /**
     * Reports something which describes the chapter merge as a whole rather than a certain pair of work items.
     */
    public void reportChapterEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull String description) {
        mergeReport.addEntry(new MergeReportEntry(operationResultType, chapterPayload(), description));
    }

    private @NotNull ChapterMergePayload chapterPayload() {
        return new ChapterMergePayload(documentInfo(sourceDocumentIdentifier), documentInfo(targetDocumentIdentifier),
                sourceChapterOutlineNumber, targetChapterOutlineNumber, insertedOutlineNumber);
    }

    private static @NotNull String documentInfo(@NotNull DocumentIdentifier documentIdentifier) {
        return "%s/%s/%s".formatted(documentIdentifier.getProjectId(), documentIdentifier.getSpaceId(), documentIdentifier.getName());
    }

    /**
     * Returns a defensive copy to avoid exposing internal mutable representation.
     */
    public List<String> getCopiedLayoutTypeIds() {
        return List.copyOf(copiedLayoutTypeIds);
    }

    @Override
    public @NonNull Map<String, IWorkItem> getItemMapping() {
        return itemMapping;
    }

    @Override
    public @NonNull Map<String, String> getCommentIdMapping() {
        return commentIdMapping;
    }
}

package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ReferencedItemsHandling;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.tracker.internal.model.IInternalWorkItem;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.subterra.base.data.identification.IContextId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentsChapterMergeServiceTest {

    private static final DocumentIdentifier SOURCE_DOCUMENT = DocumentIdentifier.builder().projectId("source").spaceId("space").name("sourceDoc").build();
    private static final DocumentIdentifier TARGET_DOCUMENT = DocumentIdentifier.builder().projectId("target").spaceId("space").name("targetDoc").build();

    @Mock
    private PolarionService polarionService;
    @Mock
    private MergeService mergeService;
    @Mock
    private DocumentsContentHandler documentsContentHandler;
    @Mock
    private DocumentLayoutSyncService documentLayoutSyncService;
    @Mock
    private CommentsCopier commentsCopier;

    private DocumentsChapterMergeService documentsChapterMergeService;

    private IModule sourceModule;
    private IModule targetModule;
    private ITrackerProject trackerProject;

    @BeforeEach
    void init() {
        documentsChapterMergeService = new DocumentsChapterMergeService(polarionService, mergeService, documentsContentHandler, documentLayoutSyncService, commentsCopier);

        trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject(anyString())).thenReturn(trackerProject);

        sourceModule = module("source", "sourceDoc");
        targetModule = module("target", "targetDoc");
        when(polarionService.getModule(SOURCE_DOCUMENT)).thenReturn(sourceModule);
        when(polarionService.getModule(TARGET_DOCUMENT)).thenReturn(targetModule);
        when(polarionService.userAuthorizedForMerge(anyString())).thenReturn(true);
        when(polarionService.getStandardFields()).thenReturn(new ArrayList<>());
        when(polarionService.getDeletableFields(any(), any(), any())).thenReturn(new ArrayList<>());
        when(documentLayoutSyncService.copyMissingLayouts(any(), any(), any()))
                .thenReturn(new DocumentLayoutSyncService.LayoutSyncResult(List.of(), List.of()));
        when(commentsCopier.copyComments(any(IWorkItem.class), any(IWorkItem.class))).thenReturn(Map.of());
        when(commentsCopier.copyComments(any(IModule.class), any(IModule.class), any(), any())).thenReturn(Map.of());
    }

    // -------------------------------------------------------------------------------------------------------------
    // guards
    // -------------------------------------------------------------------------------------------------------------

    @Test
    void testMergeFailsWhenTargetDocumentWasChangedMeanwhile() {
        DocumentIdentifier targetDocument = DocumentIdentifier.builder()
                .projectId("target").spaceId("space").name("targetDoc").moduleXmlRevision("rev1").build();
        when(polarionService.getModule(targetDocument)).thenReturn(targetModule);
        when(targetModule.getLastRevision()).thenReturn("rev2");

        MergeResult result = documentsChapterMergeService.mergeChapter(params(SOURCE_DOCUMENT, targetDocument, ChapterMergeMode.COPY, ChapterInsertMode.UNDER));

        assertFalse(result.isSuccess());
        assertTrue(result.isTargetModuleHasStructuralChanges());
    }

    @Test
    void testMergeFailsWhenUserIsNotAuthorized() {
        when(polarionService.userAuthorizedForMerge("target")).thenReturn(false);

        MergeResult result = documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER));

        assertFalse(result.isSuccess());
        assertTrue(result.isMergeNotAuthorized());
    }

    @Test
    void testMoveRequiresAuthorizationInSourceProjectToo() {
        when(polarionService.userAuthorizedForMerge("source")).thenReturn(false);

        MergeResult result = documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.MOVE, ChapterInsertMode.UNDER));

        assertFalse(result.isSuccess());
        assertTrue(result.isMergeNotAuthorized());
    }

    @Test
    void testMergeFailsWhenSourceChapterNotFound() {
        chapter(targetModule, "3.1", heading("TARGET-1"));

        MergeResult result = documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER));

        assertFalse(result.isSuccess());
        assertEquals(1, result.getMergeReport().getProhibited().size());
    }

    @Test
    void testMergeFailsWhenTargetChapterNotFound() {
        chapter(sourceModule, "2", heading("SOURCE-1"));

        MergeResult result = documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER));

        assertFalse(result.isSuccess());
        assertEquals(1, result.getMergeReport().getProhibited().size());
    }

    @Test
    void testChapterOfNonStructuralItemIsNotResolved() {
        // "-" in an outline number means that the item is not a structural part of the document
        assertNull(documentsChapterMergeService.resolveChapterNode(sourceModule, "2.1-1"));
        assertNull(documentsChapterMergeService.resolveChapterNode(sourceModule, " "));
        assertNull(documentsChapterMergeService.resolveChapterNode(sourceModule, null));
    }

    @Test
    void testChapterOfNonHeadingItemIsNotResolved() {
        IModule.IStructureNode node = node(workItem("SOURCE-1", "requirement"), false);
        when(mergeService.getNodeByOutlineNumber(sourceModule, "2")).thenReturn(node);

        assertNull(documentsChapterMergeService.resolveChapterNode(sourceModule, "2"));
    }

    @Test
    void testTargetChapterInsideSourceChapterIsProhibited() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IModule.IStructureNode targetChapter = node(heading("SOURCE-2"), true);
        when(targetChapter.getParent()).thenReturn(sourceChapter);
        when(mergeService.getNodeByOutlineNumber(sourceModule, "2.1")).thenReturn(targetChapter);

        ChapterMergeParams params = params(SOURCE_DOCUMENT, SOURCE_DOCUMENT, ChapterMergeMode.COPY, ChapterInsertMode.UNDER);
        params.setTargetChapterOutlineNumber("2.1");

        MergeResult result = documentsChapterMergeService.mergeChapter(params);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getMergeReport().getProhibited().size());
    }

    // -------------------------------------------------------------------------------------------------------------
    // copying
    // -------------------------------------------------------------------------------------------------------------

    @Test
    void testCopiedWorkItemIsNotLinkedToItsOrigin() {
        IWorkItem sourceChapterItem = heading("SOURCE-1");
        IWorkItem createdItem = trackerProjectCreates("TARGET-100", "heading");
        chapter(sourceModule, "2", sourceChapterItem);
        chapter(targetModule, "3.1", heading("TARGET-1"));

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        assertTrue(result.isSuccess());
        verify(createdItem, never()).addLinkedItem(any(), any(), any(), anyBoolean());
        verify(sourceChapterItem, never()).addLinkedItem(any(), any(), any(), anyBoolean());
        assertEquals(List.of("TARGET-100"), result.getChapterMergeInfo().getCreatedWorkItemIds());
    }

    @Test
    void testHeadingIsCreatedWithTheHeadingTypeOfTheTargetDocument() {
        chapter(sourceModule, "2", heading("SOURCE-1"));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "targetHeading");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(trackerProject).createWorkItem("targetHeading");
    }

    @Test
    void testRegularItemIsCreatedWithTheTypeOfTheSourceItem() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        addChild(sourceChapter, node(workItem("SOURCE-2", "requirement"), false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "targetHeading");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(trackerProject).createWorkItem("requirement");
    }

    @Test
    void testWholeSubtreeIsMergedInDocumentOrder() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IModule.IStructureNode subChapter = node(heading("SOURCE-2"), true);
        addChild(sourceChapter, subChapter);
        addChild(subChapter, node(workItem("SOURCE-3", "requirement"), false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        assertEquals(3, result.getChapterMergeInfo().getCreatedWorkItemIds().size());
    }

    @Test
    void testChildrenOfAWorkItemWhichCouldNotBeMergedAreSkipped() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IModule.IStructureNode subChapter = node(heading("SOURCE-2"), true);
        addChild(sourceChapter, subChapter);
        addChild(subChapter, node(workItem("SOURCE-3", "requirement"), false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        IWorkItem createdItem = trackerProjectCreates("TARGET-100", "type");
        // the target document has no node for the copy, i.e. the sub chapter never got there
        when(targetModule.getStructureNodeOfWI(createdItem)).thenReturn(null);

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        assertTrue(result.getMergeReport().getCreationFailed().stream()
                .anyMatch(entry -> entry.getDescription().contains("SOURCE-3")));
    }

    @Test
    void testFailureOfSingleWorkItemDoesNotAbortTheChapter() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        addChild(sourceChapter, node(workItem("SOURCE-2", "requirement"), false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        IWorkItem createdItem = trackerProjectCreates("TARGET-100", "type");
        doThrow(new IllegalStateException("field types differ")).when(mergeService).merge(any(), eq(createdItem), any(), any());

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        assertTrue(result.isSuccess());
        assertEquals(2, result.getMergeReport().getCreationFailed().size());
    }

    // -------------------------------------------------------------------------------------------------------------
    // moving
    // -------------------------------------------------------------------------------------------------------------

    @Test
    void testAllMovedItemsAreTakenOverInOneCall() {
        when(targetModule.getProjectId()).thenReturn("source"); // same project as the source document

        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem first = workItem("SOURCE-2", "requirement");
        IWorkItem second = workItem("SOURCE-3", "requirement");
        addChild(sourceChapter, node(first, false));
        addChild(sourceChapter, node(second, false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.MOVE, ChapterInsertMode.UNDER)));

        // Polarion rejects moving work items of one document one by one: everything after the first is answered
        // with "The resource includes or located inside the previously copied (moved) resources"
        verify(targetModule).moveIn(List.of(first, second));
        verify(mergeService, never()).insertNode(eq(first), any(), any(), anyInt(), anyBoolean());
        verify(mergeService, never()).insertNode(eq(second), any(), any(), anyInt(), anyBoolean());
        // ...and each of them is then given its position in the document
        verify(mergeService).placeNode(eq(first), eq(targetModule), any(), anyInt(), eq(false));
        verify(mergeService).placeNode(eq(second), eq(targetModule), any(), anyInt(), eq(false));
    }

    @Test
    void testHeadingsAndReferencedItemsAreNotTakenOverAsMovedOnes() {
        when(targetModule.getProjectId()).thenReturn("source");

        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem moved = workItem("SOURCE-2", "requirement");
        addChild(sourceChapter, node(moved, false));
        addChild(sourceChapter, node(workItem("OTHER-1", "requirement"), false, true));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.MOVE, ChapterInsertMode.UNDER)));

        // a heading is copied and a referenced work item stays where it is, so neither of them is moved
        verify(targetModule).moveIn(List.of(moved));
    }

    @Test
    void testNothingIsTakenOverWhenTheChapterIsCopied() {
        chapter(sourceModule, "2", heading("SOURCE-1"));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(targetModule, never()).moveIn(any());
    }

    @Test
    void testMoveWithinOneProjectMovesItemsAndCopiesHeadings() {
        when(polarionService.getModule(TARGET_DOCUMENT)).thenReturn(targetModule);
        when(targetModule.getProjectId()).thenReturn("source"); // same project as the source document

        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem movedItem = workItem("SOURCE-2", "requirement");
        addChild(sourceChapter, node(movedItem, false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.MOVE, ChapterInsertMode.UNDER)));

        assertEquals(List.of("TARGET-100"), result.getChapterMergeInfo().getCreatedWorkItemIds()); // the heading
        assertEquals(List.of("SOURCE-2"), result.getChapterMergeInfo().getMovedWorkItemIds());
        // moveIn takes the item out of the source document, so it must not be moved out explicitly
        verify(mergeService).placeNode(eq(movedItem), eq(targetModule), any(), anyInt(), eq(false));
        verify(sourceModule, never()).moveOut(any());
    }

    @Test
    void testMoveBetweenProjectsReferencesTheItemAndTakesItOutOfTheSourceDocument() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem movedItem = workItem("SOURCE-2", "requirement");
        addChild(sourceChapter, node(movedItem, false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.MOVE, ChapterInsertMode.UNDER)));

        verify(mergeService).insertNode(eq(movedItem), eq(targetModule), any(), anyInt(), eq(true));
        verify(sourceModule).moveOut(List.of(movedItem));
        assertEquals(1, result.getMergeReport().getWarnings().size());
    }

    @Test
    void testItemsAreDetachedInReverseDocumentOrder() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem first = workItem("SOURCE-2", "requirement");
        IWorkItem second = workItem("SOURCE-3", "requirement");
        addChild(sourceChapter, node(first, false));
        addChild(sourceChapter, node(second, false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.MOVE, ChapterInsertMode.UNDER)));

        InOrder order = inOrder(sourceModule);
        order.verify(sourceModule).moveOut(List.of(second));
        order.verify(sourceModule).moveOut(List.of(first));
    }

    // -------------------------------------------------------------------------------------------------------------
    // referenced work items
    // -------------------------------------------------------------------------------------------------------------

    @Test
    void testReferencedItemIsReferencedInTheTargetDocument() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem referenced = workItem("OTHER-1", "requirement");
        addChild(sourceChapter, node(referenced, false, true));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(mergeService).insertNode(eq(referenced), eq(targetModule), any(), anyInt(), eq(true));
        assertEquals(List.of("OTHER-1"), result.getChapterMergeInfo().getReferencedWorkItemIds());
    }

    @Test
    void testReferencedItemCanBeSkipped() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem referenced = workItem("OTHER-1", "requirement");
        addChild(sourceChapter, node(referenced, false, true));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        ChapterMergeParams params = params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER);
        params.setReferencedItems(ReferencedItemsHandling.SKIP);
        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params));

        verify(mergeService, never()).insertNode(eq(referenced), any(), any(), anyInt(), anyBoolean());
        assertEquals(1, result.getMergeReport().getWarnings().size());
    }

    @Test
    void testReferencedItemCanBeCopiedAsNew() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem referenced = workItem("OTHER-1", "requirement");
        addChild(sourceChapter, node(referenced, false, true));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");

        ChapterMergeParams params = params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER);
        params.setReferencedItems(ReferencedItemsHandling.COPY_AS_NEW);
        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params));

        verify(trackerProject).createWorkItem("requirement");
        assertTrue(result.getChapterMergeInfo().getReferencedWorkItemIds().isEmpty());
    }

    // -------------------------------------------------------------------------------------------------------------
    // placing the content
    // -------------------------------------------------------------------------------------------------------------

    @Test
    void testInsertionPointUnderTargetChapter() {
        IWorkItem chapterItem = heading("TARGET-1");
        IModule.IStructureNode targetChapter = node(chapterItem, true);

        DocumentsChapterMergeService.InsertionPoint insertionPoint = documentsChapterMergeService.resolveInsertionPoint(targetChapter, ChapterInsertMode.UNDER);

        assertEquals(chapterItem, insertionPoint.parentWorkItem());
        assertEquals(0, insertionPoint.index());
    }

    @Test
    void testInsertionPointAfterTargetChapter() {
        IWorkItem parentItem = heading("TARGET-0");
        IModule.IStructureNode parent = node(parentItem, true);
        IModule.IStructureNode targetChapter = node(heading("TARGET-1"), true);
        addChild(parent, node(heading("TARGET-2"), true));
        addChild(parent, targetChapter);
        addChild(parent, node(heading("TARGET-3"), true));
        when(targetChapter.getParent()).thenReturn(parent);

        DocumentsChapterMergeService.InsertionPoint insertionPoint = documentsChapterMergeService.resolveInsertionPoint(targetChapter, ChapterInsertMode.AFTER);

        assertEquals(parentItem, insertionPoint.parentWorkItem());
        assertEquals(2, insertionPoint.index());
    }

    @Test
    void testInsertionPointAfterTheLastChapterIsClamped() {
        IModule.IStructureNode parent = node(heading("TARGET-0"), true);
        IModule.IStructureNode targetChapter = node(heading("TARGET-1"), true);
        addChild(parent, targetChapter);
        when(targetChapter.getParent()).thenReturn(parent);

        DocumentsChapterMergeService.InsertionPoint insertionPoint = documentsChapterMergeService.resolveInsertionPoint(targetChapter, ChapterInsertMode.AFTER);

        assertEquals(1, insertionPoint.index());
    }

    @Test
    void testFreeContentOfTheChapterIsCopied() {
        chapter(sourceModule, "2", heading("SOURCE-1"));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "heading");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(documentsContentHandler).copyFreeContent(eq("<p>free text</p>"), eq(targetModule), eq(List.of("SOURCE-1")), any(), any());
    }

    @Test
    void testMissingWorkItemLayoutsAreCopiedIntoTheTargetDocument() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        addChild(sourceChapter, node(workItem("SOURCE-2", "requirement"), false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "type");
        when(documentLayoutSyncService.copyMissingLayouts(sourceModule, targetModule, Set.of("requirement")))
                .thenReturn(new DocumentLayoutSyncService.LayoutSyncResult(List.of("requirement"), List.of("requirement")));

        MergeResult result = inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        assertEquals(List.of("requirement"), result.getChapterMergeInfo().getCopiedLayoutTypeIds());
        assertEquals(2, result.getMergeReport().getCreated().stream()
                .filter(entry -> entry.getDescription().contains("requirement")).count());
    }

    @Test
    void testWorkItemLayoutsAreNotCopiedWhenTheOptionIsOff() {
        chapter(sourceModule, "2", heading("SOURCE-1"));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "heading");

        ChapterMergeParams params = params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER);
        params.setCopyWorkItemLayouts(false);
        inTransaction(() -> documentsChapterMergeService.mergeChapter(params));

        verify(documentLayoutSyncService, never()).copyMissingLayouts(any(), any(), any());
    }


    @Test
    void testAFieldTheSourceWorkItemHasNoValueForIsNotCopied() {
        IWorkItem sourceWorkItem = workItem("SOURCE-1", "heading");
        when(polarionService.getDeletableFields(any(), any(), any())).thenReturn(new ArrayList<>(List.of(
                WorkItemField.builder().key("title").build(),
                WorkItemField.builder().key("severity").build(),
                WorkItemField.builder().key("module").build())));
        when(polarionService.getFieldValue(sourceWorkItem, "title")).thenReturn("A chapter");
        when(polarionService.getFieldValue(sourceWorkItem, "severity")).thenReturn(null);

        List<DiffField> fields = documentsChapterMergeService.fieldsToCopy(sourceWorkItem, sourceModule).getDiffFields();

        // a chapter heading has no severity to give, and a required field cannot be set to an empty value
        assertEquals(List.of("title"), fields.stream().map(DiffField::getKey).toList());
    }


    @Test
    void testACopiedWorkItemGetsTheCommentsOfTheItemItWasCopiedFrom() {
        IWorkItem sourceChapterItem = heading("SOURCE-1");
        IWorkItem createdItem = trackerProjectCreates("TARGET-100", "heading");
        chapter(sourceModule, "2", sourceChapterItem);
        chapter(targetModule, "3.1", heading("TARGET-1"));

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(commentsCopier).copyComments(sourceChapterItem, createdItem);
    }

    @Test
    void testTheCommentsOfACopyAreThereBeforeTheTextWhichNamesThem() {
        IWorkItem sourceChapterItem = heading("SOURCE-1");
        IWorkItem createdItem = trackerProjectCreates("TARGET-100", "heading");
        chapter(sourceModule, "2", sourceChapterItem);
        chapter(targetModule, "3.1", heading("TARGET-1"));
        DocumentsChapterMergeContext[] usedContext = new DocumentsChapterMergeContext[1];
        when(commentsCopier.copyComments(sourceChapterItem, createdItem)).thenReturn(Map.of("5", "17"));
        doAnswer(invocation -> {
            usedContext[0] = invocation.getArgument(2);
            // the fields are copied knowing which comment of the copy a marker of the source text names
            assertEquals(Map.of("5", "17"), usedContext[0].getCommentIdMapping());
            return null;
        }).when(mergeService).merge(eq(sourceChapterItem), eq(createdItem), any(), any());

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        InOrder order = inOrder(commentsCopier, mergeService);
        order.verify(commentsCopier).copyComments(sourceChapterItem, createdItem);
        order.verify(mergeService).merge(eq(sourceChapterItem), eq(createdItem), any(), any());
    }

    @Test
    void testTheCommentsWrittenOnTheCopiedTextAreCopiedIntoTheTargetDocument() {
        DocumentsChapterMergeContext context = mock(DocumentsChapterMergeContext.class);
        when(context.getSourceHomePageContentSnapshot()).thenReturn("<p>text</p>");
        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);
        when(documentsContentHandler.freeContentCommentIds("<p>text</p>", List.of("SOURCE-1"))).thenReturn(Set.of("5"));
        when(commentsCopier.copyComments(eq(sourceModule), eq(targetModule), any(), any())).thenReturn(Map.of("5", "17"));

        assertEquals(Map.of("5", "17"), documentsChapterMergeService.copyDocumentComments(context, List.of("SOURCE-1")));
    }

    @Test
    void testTextWithoutCommentsCopiesNoDocumentComments() {
        DocumentsChapterMergeContext context = mock(DocumentsChapterMergeContext.class);
        when(context.getSourceHomePageContentSnapshot()).thenReturn("<p>text</p>");
        when(documentsContentHandler.freeContentCommentIds(anyString(), any())).thenReturn(Set.of());

        assertTrue(documentsChapterMergeService.copyDocumentComments(context, List.of("SOURCE-1")).isEmpty());
        verify(commentsCopier, never()).copyComments(any(IModule.class), any(IModule.class), any(), any());
    }


    @Test
    void testMergedWorkItemsArePlacedUnderTheTargetChapter() {
        chapter(sourceModule, "2", heading("SOURCE-1"));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "heading");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(documentsContentHandler).moveAnchorsBelow(targetModule, List.of("TARGET-100"), "TARGET-1");
    }

    @Test
    void testAChapterMergedAfterAnotherOneIsLeftWhereItWasPlaced() {
        chapter(sourceModule, "2", heading("SOURCE-1"));
        IModule.IStructureNode targetChapter = chapter(targetModule, "3.1", heading("TARGET-1"));
        IModule.IStructureNode parentChapter = node(heading("TARGET-0"), true);
        when(targetChapter.getParent()).thenReturn(parentChapter);
        trackerProjectCreates("TARGET-100", "heading");

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.AFTER)));

        // it follows the whole target chapter, so it is not part of what that chapter holds
        verify(documentsContentHandler, never()).moveAnchorsBelow(any(), any(), anyString());
    }

    @Test
    void testAMergeWhichPlacedNothingMovesNothing() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        addChild(sourceChapter, node(workItem("SOURCE-2", "requirement"), false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        IWorkItem createdItem = trackerProjectCreates("TARGET-100", "heading");
        doThrow(new IllegalStateException("boom")).when(mergeService).merge(any(), eq(createdItem), any(), any());

        inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER)));

        verify(documentsContentHandler, never()).moveAnchorsBelow(any(), any(), anyString());
    }


    @Test
    void testTheWholeMergeIsOneTransaction() {
        chapter(sourceModule, "2", heading("SOURCE-1"));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "heading");

        try (MockedStatic<TransactionalExecutor> transactionalExecutor = mockStatic(TransactionalExecutor.class)) {
            transactionalExecutor.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(invocation -> {
                RunnableInWriteTransaction<?> runnable = invocation.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });

            documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.COPY, ChapterInsertMode.UNDER));

            // Placing the work items and writing the page belong together: a merge which committed the first and
            // failed at the second would leave workitems behind, and in move mode take them off the source document
            transactionalExecutor.verify(() -> TransactionalExecutor.executeInWriteTransaction(any()), times(1));
        }
    }

    @Test
    void testAFailureAfterTheWorkItemsWerePlacedFailsTheWholeMerge() {
        IModule.IStructureNode sourceChapter = chapter(sourceModule, "2", heading("SOURCE-1"));
        IWorkItem movedItem = workItem("SOURCE-2", "requirement");
        addChild(sourceChapter, node(movedItem, false));
        chapter(targetModule, "3.1", heading("TARGET-1"));
        trackerProjectCreates("TARGET-100", "heading");
        when(documentsContentHandler.copyFreeContent(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("boom"));

        // Nothing is caught here, so the transaction is left to roll everything back
        assertThrows(IllegalStateException.class,
                () -> inTransaction(() -> documentsChapterMergeService.mergeChapter(params(ChapterMergeMode.MOVE, ChapterInsertMode.UNDER))));
    }

    // -------------------------------------------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------------------------------------------

    private ChapterMergeParams params(@SuppressWarnings("SameParameterValue") ChapterMergeMode mode, ChapterInsertMode insertMode) {
        return params(SOURCE_DOCUMENT, TARGET_DOCUMENT, mode, insertMode);
    }

    private ChapterMergeParams params(DocumentIdentifier sourceDocument, DocumentIdentifier targetDocument, ChapterMergeMode mode, ChapterInsertMode insertMode) {
        return ChapterMergeParams.builder()
                .sourceDocument(sourceDocument)
                .targetDocument(targetDocument)
                .mode(mode)
                .insertMode(insertMode)
                .sourceChapterOutlineNumber("2")
                .targetChapterOutlineNumber("3.1")
                .copyWorkItemLayouts(true)
                .build();
    }

    private MergeResult inTransaction(java.util.function.Supplier<MergeResult> merge) {
        try (MockedStatic<TransactionalExecutor> transactionalExecutor = mockStatic(TransactionalExecutor.class)) {
            transactionalExecutor.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(invocation -> {
                RunnableInWriteTransaction<?> runnable = invocation.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });
            MergeResult result = merge.get();
            assertNotNull(result);
            return result;
        }
    }

    private IModule module(String projectId, String name) {
        IModule module = mock(IModule.class);
        when(module.getProjectId()).thenReturn(projectId);
        when(module.getModuleName()).thenReturn(name);
        when(module.getHomePageContent()).thenReturn(Text.html("<p>free text</p>"));
        when(module.getExternalWorkItems()).thenReturn(new ArrayList<>());
        ITypeOpt headingType = mock(ITypeOpt.class);
        when(headingType.getId()).thenReturn("targetHeading");
        when(module.getHeadingWorkItemType()).thenReturn(headingType);
        ITrackerProject project = mock(ITrackerProject.class);
        when(project.getContextId()).thenReturn(mock(IContextId.class));
        when(module.getProject()).thenReturn(project);
        return module;
    }

    private IWorkItem trackerProjectCreates(String createdId, String typeId) {
        IWorkItem created = workItem(createdId, typeId);
        when(trackerProject.createWorkItem(anyString())).thenReturn(created);
        // Once an item is placed, the target document knows the node it sits in - which is what its children need
        IModule.IStructureNode createdNode = node(created, false);
        when(targetModule.getStructureNodeOfWI(created)).thenReturn(createdNode);
        return created;
    }

    private IWorkItem workItem(String id, String typeId) {
        IInternalWorkItem workItem = mock(IInternalWorkItem.class);
        when(workItem.getId()).thenReturn(id);
        ITypeOpt type = mock(ITypeOpt.class);
        when(type.getId()).thenReturn(typeId);
        when(workItem.getType()).thenReturn(type);
        return workItem;
    }

    private IWorkItem heading(String id) {
        IWorkItem workItem = workItem(id, "heading");
        when(((IInternalWorkItem) workItem).isHeading()).thenReturn(true);
        return workItem;
    }

    private IModule.IStructureNode node(IWorkItem workItem, boolean heading) {
        return node(workItem, heading, false);
    }

    private IModule.IStructureNode node(IWorkItem workItem, boolean heading, boolean external) {
        IModule.IStructureNode node = mock(IModule.IStructureNode.class);
        when(node.getWorkItem()).thenReturn(workItem);
        when(node.getChildren()).thenReturn(new ArrayList<>());
        when(node.isExternal()).thenReturn(external);
        if (workItem != null && heading) {
            when(((IInternalWorkItem) workItem).isHeading()).thenReturn(true);
        }
        return node;
    }

    private IModule.IStructureNode chapter(IModule module, String outlineNumber, IWorkItem workItem) {
        IModule.IStructureNode node = node(workItem, true);
        when(mergeService.getNodeByOutlineNumber(module, outlineNumber)).thenReturn(node);
        return node;
    }

    private void addChild(IModule.IStructureNode parent, IModule.IStructureNode child) {
        parent.getChildren().add(child);
    }
}

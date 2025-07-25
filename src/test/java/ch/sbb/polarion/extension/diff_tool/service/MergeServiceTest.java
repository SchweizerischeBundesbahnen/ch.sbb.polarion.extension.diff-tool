package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergePair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Project;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.LinkRole;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.api.model.document.internal.InternalUpdatableDocument;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.shared.api.utils.collections.StrictList;
import com.polarion.alm.shared.dle.compare.DleWIsMergeAction;
import com.polarion.alm.shared.dle.compare.DleWIsMergeActionExecuter;
import com.polarion.alm.shared.dle.compare.DleWorkItemsComparator;
import com.polarion.alm.tracker.internal.model.HyperlinkStruct;
import com.polarion.alm.tracker.internal.model.IInternalWorkItem;
import com.polarion.alm.tracker.model.IHyperlinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IStructType;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("diff-tool")
@SuppressWarnings({"unchecked", "rawtypes"})
class MergeServiceTest {
    @Mock
    private PolarionService polarionService;
    @Mock
    private SettingsService settingsService;

    private MergeService mergeService;

    @BeforeEach
    void init() {
        mergeService = new MergeService(polarionService);
    }

    @AfterEach
    void tearDown() {
        // Unregister our mock settings
        NamedSettingsRegistry.INSTANCE.getAll().clear();
    }

    @Test
    void testMergeFailedDueToStructuralChanges() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));

        DocumentIdentifier documentIdentifier1 = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("doc1").revision("rev1").moduleXmlRevision("rev1").build();
        DocumentIdentifier documentIdentifier2 = DocumentIdentifier.builder().projectId("project2").spaceId("space2").name("doc2").revision("rev2").moduleXmlRevision("rev2").build();

        IModule module1 = mock(IModule.class);
        ILocation location1 = mock(ILocation.class);
        when(location1.getLocationPath()).thenReturn("space1/doc1");
        when(module1.getModuleLocation()).thenReturn(location1);
        when(polarionService.getModule(documentIdentifier1)).thenReturn(module1);

        IModule module2 = mock(IModule.class);
        ILocation location2 = mock(ILocation.class);
        when(location2.getLocationPath()).thenReturn("space2/doc2");
        when(module2.getModuleLocation()).thenReturn(location2);
        when(module2.getLastRevision()).thenReturn("rev3");
        when(polarionService.getModule(documentIdentifier2)).thenReturn(module2);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class)) {
            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));
            DocumentsMergeParams mergeParams = new DocumentsMergeParams(documentIdentifier1, documentIdentifier2, MergeDirection.LEFT_TO_RIGHT,
                    "any", null, "any", Collections.emptyList(), false);
            MergeResult mergeResult = mergeService.mergeDocuments(mergeParams);
            assertFalse(mergeResult.isSuccess());
            assertTrue(mergeResult.isTargetModuleHasStructuralChanges());
        }
    }

    @Test
    void testDocumentsContentMergeFailedDueToStructuralChanges() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));

        DocumentIdentifier documentIdentifier1 = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("doc1").revision("rev1").moduleXmlRevision("rev1").build();
        DocumentIdentifier documentIdentifier2 = DocumentIdentifier.builder().projectId("project2").spaceId("space2").name("doc2").revision("rev2").moduleXmlRevision("rev2").build();

        IModule module1 = mock(IModule.class);
        when(polarionService.getModule(documentIdentifier1)).thenReturn(module1);

        IModule module2 = mock(IModule.class);
        when(module2.getLastRevision()).thenReturn("rev3");
        when(polarionService.getModule(documentIdentifier2)).thenReturn(module2);

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class)) {
            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));
            DocumentsContentMergeParams mergeParams = new DocumentsContentMergeParams(documentIdentifier1, documentIdentifier2, MergeDirection.LEFT_TO_RIGHT,
                    Collections.emptyList());
            MergeResult mergeResult = mergeService.mergeDocumentsContent(mergeParams);
            assertFalse(mergeResult.isSuccess());
            assertTrue(mergeResult.isTargetModuleHasStructuralChanges());
        }
    }

    @Test
    void testDocumentsFieldsMergeFailedDueToStructuralChanges() {
        IModule source = mock(IModule.class);
        IModule target = mock(IModule.class);
        when(target.getLastRevision()).thenReturn("rev3");
        when(polarionService.getModule(any())).thenReturn(source, target);

        DocumentIdentifier documentIdentifier1 = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("doc1").revision("rev1").moduleXmlRevision("rev1").build();
        DocumentIdentifier documentIdentifier2 = DocumentIdentifier.builder().projectId("project2").spaceId("space2").name("doc2").revision("rev2").moduleXmlRevision("rev2").build();

        MergeResult mergeResult = mergeService.mergeDocumentsFields(new DocumentsFieldsMergeParams(documentIdentifier1, documentIdentifier2,
                MergeDirection.LEFT_TO_RIGHT, List.of("a", "b", "c")));

        assertFalse(mergeResult.isSuccess());
        assertTrue(mergeResult.isTargetModuleHasStructuralChanges());
    }

    @Test
    void testMergeFailedDueToNotAuthorized() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));

        DocumentIdentifier documentIdentifier1 = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("doc1").revision("rev1").moduleXmlRevision("rev1").build();
        DocumentIdentifier documentIdentifier2 = DocumentIdentifier.builder().projectId("project2").spaceId("space2").name("doc2").revision("rev2").moduleXmlRevision("rev2").build();

        IModule module1 = mock(IModule.class);
        ILocation location1 = mock(ILocation.class);
        when(location1.getLocationPath()).thenReturn("space1/doc1");
        when(module1.getModuleLocation()).thenReturn(location1);
        when(polarionService.getModule(documentIdentifier1)).thenReturn(module1);

        IModule module2 = mock(IModule.class);
        ILocation location2 = mock(ILocation.class);
        when(location2.getLocationPath()).thenReturn("space2/doc2");
        when(module2.getModuleLocation()).thenReturn(location2);
        when(module2.getLastRevision()).thenReturn("rev2");
        when(polarionService.getModule(documentIdentifier2)).thenReturn(module2);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));

        when(polarionService.userAuthorizedForMerge(any())).thenReturn(false);

        DocumentsMergeParams mergeParams = new DocumentsMergeParams(documentIdentifier1, documentIdentifier2, MergeDirection.LEFT_TO_RIGHT,
                "any", null, "any", Collections.emptyList(), false);

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class)) {
            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));
            MergeResult mergeResult = mergeService.mergeDocuments(mergeParams);
            assertFalse(mergeResult.isSuccess());
            assertTrue(mergeResult.isMergeNotAuthorized());
        }
    }

    @Test
    void testDocumentsContentMergeFailedDueToNotAuthorized() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));

        DocumentIdentifier documentIdentifier1 = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("doc1").revision("rev1").moduleXmlRevision("rev1").build();
        DocumentIdentifier documentIdentifier2 = DocumentIdentifier.builder().projectId("project2").spaceId("space2").name("doc2").revision("rev2").moduleXmlRevision("rev2").build();

        IModule module1 = mock(IModule.class);
        when(polarionService.getModule(documentIdentifier1)).thenReturn(module1);

        IModule module2 = mock(IModule.class);
        when(module2.getLastRevision()).thenReturn("rev2");
        when(polarionService.getModule(documentIdentifier2)).thenReturn(module2);

        when(polarionService.userAuthorizedForMerge(any())).thenReturn(false);

        DocumentsContentMergeParams mergeParams = new DocumentsContentMergeParams(documentIdentifier1, documentIdentifier2, MergeDirection.LEFT_TO_RIGHT,
                Collections.emptyList());

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class)) {
            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));
            MergeResult mergeResult = mergeService.mergeDocumentsContent(mergeParams);
            assertFalse(mergeResult.isSuccess());
            assertTrue(mergeResult.isMergeNotAuthorized());
        }
    }

    @Test
    void testMergeDocumentsSuccess() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");

        IModule leftModule = mock(IModule.class, RETURNS_DEEP_STUBS);
        ILocation location = mock(ILocation.class);
        when(location.getLocationPath()).thenReturn("space1/doc1");
        when(leftModule.getModuleLocation()).thenReturn(location);
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getModuleLocation()).thenReturn(location);
        when(rightModule.getLastRevision()).thenReturn("rev1");
        when(rightModule.getProjectId()).thenReturn("project1");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);


        IWorkItem leftWorkItem = mock(IWorkItem.class);
        when(leftWorkItem.getId()).thenReturn("left");
        IPObjectList iPObjectList = mock(IPObjectList.class);
        when(leftWorkItem.getLinkedWorkItems()).thenReturn(iPObjectList);

        IWorkItem rightWorkItem = mock(IWorkItem.class);
        when(rightWorkItem.getId()).thenReturn("right");
        List<WorkItemsPair> workItemsPairs = new ArrayList<>();

        WorkItem sourceWorkItem = WorkItem.of(leftWorkItem);
        sourceWorkItem.setLastRevision("rev1");
        WorkItem targetWorkItem = WorkItem.of(rightWorkItem);
        targetWorkItem.setLastRevision("rev1");

        WorkItemsPair workItemsPair = WorkItemsPair.builder().leftWorkItem(sourceWorkItem).rightWorkItem(targetWorkItem).build();
        workItemsPairs.add(workItemsPair);
        DocumentsMergeParams mergeParams = new DocumentsMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, "any", null, "any", workItemsPairs, true);

        when(polarionService.userAuthorizedForMerge(anyString())).thenReturn(true);

        when(mergeService.getWorkItem(workItemsPair.getLeftWorkItem())).thenReturn(leftWorkItem);
        when(mergeService.getWorkItem(workItemsPair.getRightWorkItem())).thenReturn(rightWorkItem);

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class); MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(arg -> {
                RunnableInWriteTransaction<?> runnable = arg.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });

            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));
            when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));

            MergeService service = spy(mergeService);
            doNothing().when(service).reloadModule(rightModule);

            MergeResult result = service.mergeDocuments(mergeParams);
            assertNotNull(result);
        }
    }

    @Test
    void testDocumentsContentMergeSuccess() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");

        IModule leftModule = mock(IModule.class, RETURNS_DEEP_STUBS);
        ILocation location = mock(ILocation.class);
        when(leftModule.getModuleLocation()).thenReturn(location);
        when(leftModule.getHomePageContent()).thenReturn(Text.html("""
                <p id="polarion_1">Some text</p><div id="polarion_wiki macro name=module-workitem;params=id=left"></div>
        """));
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getHomePageContent()).thenReturn(Text.html("""
                <p id="polarion_1">Inserted paragraph</p><div id="polarion_wiki macro name=module-workitem;params=id=right"></div>
        """));
        when(rightModule.getLastRevision()).thenReturn("rev1");
        when(rightModule.getProjectId()).thenReturn("project1");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);

        List<DocumentsContentMergePair> workItemsPairs = new ArrayList<>();
        DocumentsContentMergePair workItemsPair = DocumentsContentMergePair.builder().leftWorkItemId("left").rightWorkItemId("right").contentPosition(DocumentContentAnchor.ContentPosition.ABOVE).build();
        workItemsPairs.add(workItemsPair);

        DocumentsContentMergeParams mergeParams = new DocumentsContentMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, workItemsPairs);

        when(polarionService.userAuthorizedForMerge(anyString())).thenReturn(true);

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class); MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(arg -> {
                RunnableInWriteTransaction<?> runnable = arg.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });

            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));

            MergeService service = spy(mergeService);

            MergeResult result = service.mergeDocumentsContent(mergeParams);
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(1, result.getMergeReport().getModified().size());
        }
    }

    @Test
    void testDocumentsContentMergeNothing() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");

        IModule leftModule = mock(IModule.class, RETURNS_DEEP_STUBS);
        ILocation location = mock(ILocation.class);
        when(leftModule.getModuleLocation()).thenReturn(location);
        when(leftModule.getHomePageContent()).thenReturn(Text.html("""
                <p id="polarion_1">Some text</p><div id="polarion_wiki macro name=module-workitem;params=id=left"></div>
        """));
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getHomePageContent()).thenReturn(Text.html("""
                <p id="polarion_1">Inserted paragraph</p><div id="polarion_wiki macro name=module-workitem;params=id=right"></div>
        """));
        when(rightModule.getLastRevision()).thenReturn("rev1");
        when(rightModule.getProjectId()).thenReturn("project1");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);

        DocumentsContentMergeParams mergeParams = new DocumentsContentMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, Collections.emptyList());

        when(polarionService.userAuthorizedForMerge(anyString())).thenReturn(true);

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class); MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(arg -> {
                RunnableInWriteTransaction<?> runnable = arg.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });

            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));

            MergeService service = spy(mergeService);

            MergeResult result = service.mergeDocumentsContent(mergeParams);
            assertNotNull(result);
            assertFalse(result.isSuccess());
        }
    }

    @Test
    void updateAndMoveItemAwaitModifyAndSkippedMove() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project1", "space1", "doc2", "rev1", "rev1");

        IModule leftModule = mock(IModule.class);
        ILocation leftLocation = mock(ILocation.class);
        when(leftLocation.getLocationPath()).thenReturn("space1/doc1");
        when(leftModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(rightModule.getLastRevision()).thenReturn("rev1");
        lenient().when(rightModule.getProjectId()).thenReturn("project1");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);

        IWorkItem leftWorkItem = mock(IWorkItem.class);
        when(leftWorkItem.getId()).thenReturn("left");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getLastRevision()).thenReturn("rev1");

        when(leftModule.getOutlineNumberOfWorkitem(leftWorkItem)).thenReturn("1");

        IWorkItem rightWorkItem = mock(IWorkItem.class);
        when(rightWorkItem.getId()).thenReturn("right");
        when(rightWorkItem.getProjectId()).thenReturn("project1");
        when(rightWorkItem.getLastRevision()).thenReturn("rev1");

        when(rightModule.getOutlineNumberOfWorkitem(rightWorkItem)).thenReturn("1");

        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        lenient().when(leftModule.getStructureNodeOfWI(leftWorkItem)).thenReturn(sourceNode);

        IModule.IStructureNode targetNode = mock(IModule.IStructureNode.class);
        lenient().when(rightModule.getStructureNodeOfWI(rightWorkItem)).thenReturn(targetNode);

        List<WorkItemsPair> workItemsPairs = new ArrayList<>();
        workItemsPairs.add(WorkItemsPair.of(leftWorkItem, rightWorkItem));
        DiffModel diffModel = mock(DiffModel.class);

        List<DiffField> diffFields = new ArrayList<>();
        diffFields.add(DiffField.builder().key("title").build());
        diffFields.add(DiffField.builder().key("title").build());

        when(diffModel.getDiffFields()).thenReturn(diffFields);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));
        DocumentsMergeParams mergeParams = new DocumentsMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, "any", null, "any", workItemsPairs, false);
        DocumentsMergeContext context = new DocumentsMergeContext(polarionService, doc1, doc2, mergeParams.getDirection(), mergeParams.getLinkRole(), diffModel, mergeParams.isAllowedReferencedWorkItemMerge());

        when(polarionService.getWorkItem("project1", "left", null)).thenReturn(leftWorkItem);
        when(polarionService.getWorkItem("project1", "right", null)).thenReturn(rightWorkItem);

        WorkItemsPair pair = WorkItemsPair.of(leftWorkItem, rightWorkItem);
        pair.getLeftWorkItem().setMovedOutlineNumber("1");

        mergeService.updateAndMoveItem(pair, context);
        assertNotNull(context.getMergeReport().getEntriesByType(MODIFIED));
        assertNotNull(context.getMergeReport().getEntriesByType(MOVE_SKIPPED));
    }

    @Test
    void updateAndMoveItemAwaitModifyAndMoved() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project1", "space1", "doc2", "rev1", "rev1");

        IModule leftModule = mock(IModule.class);
        ILocation leftLocation = mock(ILocation.class);
        when(leftLocation.getLocationPath()).thenReturn("space1/doc1");
        when(leftModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(rightModule.getLastRevision()).thenReturn("rev1");
        lenient().when(rightModule.getProjectId()).thenReturn("project1");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);

        IWorkItem leftWorkItem = mock(IWorkItem.class);
        when(leftWorkItem.getId()).thenReturn("left");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getLastRevision()).thenReturn("rev1");

        when(leftModule.getOutlineNumberOfWorkitem(leftWorkItem)).thenReturn("2");
        lenient().when(leftModule.getAllWorkItems()).thenReturn(List.of(leftWorkItem));

        IWorkItem rightWorkItem = mock(IWorkItem.class);
        when(rightWorkItem.getId()).thenReturn("right");
        when(rightWorkItem.getProjectId()).thenReturn("project1");
        when(rightWorkItem.getLastRevision()).thenReturn("rev1");

        when(rightModule.getOutlineNumberOfWorkitem(rightWorkItem)).thenReturn("1");
        when(rightModule.getAllWorkItems()).thenReturn(List.of(rightWorkItem));

        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        lenient().when(sourceNode.getOutlineNumber()).thenReturn("1");
        lenient().when(leftModule.getStructureNodeOfWI(leftWorkItem)).thenReturn(sourceNode);
        IModule.IStructureNode parentNode = mock(IModule.IStructureNode.class);
        when(sourceNode.getParent()).thenReturn(parentNode);
        IWorkItem parentWorkItem = mock(IWorkItem.class);
        when(parentNode.getWorkItem()).thenReturn(parentWorkItem);

        IModule.IStructureNode targetNode = mock(IModule.IStructureNode.class);
        lenient().when(rightModule.getStructureNodeOfWI(rightWorkItem)).thenReturn(targetNode);
        IModule.IStructureNode parentTargetNode = mock(IModule.IStructureNode.class);
        when(targetNode.getParent()).thenReturn(parentTargetNode);
        IWorkItem targetParentWorkItem = mock(IWorkItem.class);
        when(parentTargetNode.getWorkItem()).thenReturn(targetParentWorkItem);

        List<WorkItemsPair> workItemsPairs = new ArrayList<>();
        workItemsPairs.add(WorkItemsPair.of(leftWorkItem, rightWorkItem));
        DiffModel diffModel = mock(DiffModel.class);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));
        DocumentsMergeParams mergeParams = new DocumentsMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, "any", null, "any", workItemsPairs, false);
        DocumentsMergeContext context = new DocumentsMergeContext(polarionService, doc1, doc2, mergeParams.getDirection(), mergeParams.getLinkRole(), diffModel, mergeParams.isAllowedReferencedWorkItemMerge());

        when(polarionService.getWorkItem("project1", "left", null)).thenReturn(leftWorkItem);
        when(polarionService.getWorkItem("project1", "right", null)).thenReturn(rightWorkItem);

        WorkItemsPair pair = WorkItemsPair.of(leftWorkItem, rightWorkItem);
        pair.getLeftWorkItem().setMovedOutlineNumber("1");

        mergeService.updateAndMoveItem(pair, context);
        assertNotNull(context.getMergeReport().getEntriesByType(MODIFIED));
    }

    @Test
    void updateAndMoveItemAwaitFixReferenced() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project1", "space1", "doc2", "rev1", "rev1");

        IModule leftModule = mock(IModule.class);
        ILocation leftLocation = mock(ILocation.class);
        when(leftLocation.getLocationPath()).thenReturn("space1/doc1");
        when(leftModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(rightModule.getLastRevision()).thenReturn("rev1");
        lenient().when(rightModule.getProjectId()).thenReturn("project2");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);

        IWorkItem leftWorkItem = mock(IWorkItem.class);
        when(leftWorkItem.getId()).thenReturn("left");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getLastRevision()).thenReturn("rev1");

        when(leftModule.getOutlineNumberOfWorkitem(leftWorkItem)).thenReturn("2");
        lenient().when(leftModule.getAllWorkItems()).thenReturn(List.of(leftWorkItem));

        IWorkItem rightWorkItem = mock(IWorkItem.class);
        when(rightWorkItem.getId()).thenReturn("right");
        when(rightWorkItem.getProjectId()).thenReturn("project1");
        when(rightWorkItem.getLastRevision()).thenReturn("rev1");

        when(rightModule.getOutlineNumberOfWorkitem(rightWorkItem)).thenReturn("1");
        when(rightModule.getAllWorkItems()).thenReturn(List.of(rightWorkItem));
        when(rightModule.getExternalWorkItems()).thenReturn(List.of(rightWorkItem));

        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        lenient().when(sourceNode.getOutlineNumber()).thenReturn("1");
        lenient().when(leftModule.getStructureNodeOfWI(leftWorkItem)).thenReturn(sourceNode);
        IModule.IStructureNode parentNode = mock(IModule.IStructureNode.class);
        when(sourceNode.getParent()).thenReturn(parentNode);
        IWorkItem parentWorkItem = mock(IWorkItem.class);
        when(parentNode.getWorkItem()).thenReturn(parentWorkItem);

        IModule.IStructureNode targetNode = mock(IModule.IStructureNode.class);
        lenient().when(rightModule.getStructureNodeOfWI(rightWorkItem)).thenReturn(targetNode);
        IModule.IStructureNode parentTargetNode = mock(IModule.IStructureNode.class);
        when(targetNode.getParent()).thenReturn(parentTargetNode);
        IWorkItem targetParentWorkItem = mock(IWorkItem.class);
        when(parentTargetNode.getWorkItem()).thenReturn(targetParentWorkItem);

        List<WorkItemsPair> workItemsPairs = new ArrayList<>();
        workItemsPairs.add(WorkItemsPair.of(leftWorkItem, rightWorkItem));
        DiffModel diffModel = mock(DiffModel.class);
        ILinkRoleOpt ilinkRoleOpt = mock(ILinkRoleOpt.class);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(ilinkRoleOpt);
        DocumentsMergeParams mergeParams = new DocumentsMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, "any", null, "any", workItemsPairs, false);
        DocumentsMergeContext context = new DocumentsMergeContext(polarionService, doc1, doc2, mergeParams.getDirection(), mergeParams.getLinkRole(), diffModel, mergeParams.isAllowedReferencedWorkItemMerge());

        when(polarionService.getWorkItem("project1", "left", null)).thenReturn(leftWorkItem);
        when(polarionService.getWorkItem("project1", "right", null)).thenReturn(rightWorkItem);

        WorkItemsPair pair = WorkItemsPair.of(leftWorkItem, rightWorkItem);
        pair.getLeftWorkItem().setMovedOutlineNumber("1");

        MergeService service = spy(mergeService);
        doNothing().when(service).reloadModule(context.getTargetModule());

        service.updateAndMoveItem(pair, context);
        assertNotNull(context.getMergeReport().getEntriesByType(MODIFIED));
        verify(polarionService).fixReferencedWorkItem(rightWorkItem, rightModule, ilinkRoleOpt);
    }

    @Test
    void testMoveWorkItemOutOfDocument() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project1", "space1", "doc2", "rev1", "rev1");

        IModule leftModule = mock(IModule.class);
        ILocation leftLocation = mock(ILocation.class);
        when(leftLocation.getLocationPath()).thenReturn("space1/doc1");
        when(leftModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(rightModule.getLastRevision()).thenReturn("rev1");
        lenient().when(rightModule.getProjectId()).thenReturn("project1");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);

        IWorkItem leftWorkItem = mock(IWorkItem.class);
        when(leftWorkItem.getId()).thenReturn("left");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getLastRevision()).thenReturn("rev1");

        lenient().when(leftModule.getAllWorkItems()).thenReturn(List.of(leftWorkItem));

        IWorkItem rightWorkItem = mock(IWorkItem.class);
        when(rightWorkItem.getId()).thenReturn("right");
        when(rightWorkItem.getProjectId()).thenReturn("project1");
        when(rightWorkItem.getLastRevision()).thenReturn("rev1");

        WorkItemsPair pair = WorkItemsPair.of(leftWorkItem, rightWorkItem);
        pair.getLeftWorkItem().setReferenced(true);

        DiffModel diffModel = mock(DiffModel.class);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));

        DocumentsMergeParams mergeParams = new DocumentsMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, "any", null, "any", List.of(pair), true);
        DocumentsMergeContext context = new DocumentsMergeContext(polarionService, doc1, doc2, mergeParams.getDirection(), mergeParams.getLinkRole(), diffModel, mergeParams.isAllowedReferencedWorkItemMerge());

        when(mergeService.getWorkItem(pair.getLeftWorkItem())).thenReturn(leftWorkItem);
        when(mergeService.getWorkItem(pair.getRightWorkItem())).thenReturn(rightWorkItem);

        IPObjectList iPObjectList = mock(IPObjectList.class);
        when(iPObjectList.contains(rightWorkItem)).thenReturn(true);
        when(leftWorkItem.getLinkedWorkItems()).thenReturn(iPObjectList);

        when(polarionService.getModule(doc2.getProjectId(), doc2.getSpaceId(), doc2.getName())).thenReturn(rightModule);

        boolean result = mergeService.moveWorkItemOutOfDocument(pair, context);
        assertNull(pair.getRightWorkItem());
        assertNotNull(context.getMergeReport().getEntriesByType(DETACHED));
        assertFalse(result);
    }

    @Test
    void testMergeWorkItemsSuccessfulMerge() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        WorkItemsMergeParams mergeParams = mock(WorkItemsMergeParams.class);
        Project leftProject = mock(Project.class);
        Project rightProject = mock(Project.class);
        when(mergeParams.getLeftProject()).thenReturn(leftProject);
        when(mergeParams.getRightProject()).thenReturn(rightProject);
        when(mergeParams.getDirection()).thenReturn(MergeDirection.LEFT_TO_RIGHT);
        when(mergeParams.getLinkRole()).thenReturn("reuse");
        when(mergeParams.getConfigName()).thenReturn("config");
        when(mergeParams.getConfigCacheBucketId()).thenReturn("ConfigCacheBucketId");
        when(leftProject.getId()).thenReturn("projectId");
        when(rightProject.getId()).thenReturn("projectId");
        when(polarionService.userAuthorizedForMerge("projectId")).thenReturn(true);

        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class); MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(mock(DiffModel.class));

            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(arg -> {
                RunnableInWriteTransaction<?> runnable = arg.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });

            List<WorkItemsPair> pairs = new ArrayList<>();
            WorkItemsPair pair1 = mock(WorkItemsPair.class);
            WorkItem leftItem1 = mock(WorkItem.class);
            WorkItem rightItem1 = mock(WorkItem.class);
            when(pair1.getLeftWorkItem()).thenReturn(leftItem1);
            when(pair1.getRightWorkItem()).thenReturn(rightItem1);

            pairs.add(pair1);
            when(mergeParams.getPairs()).thenReturn(pairs);

            ILinkRoleOpt linkRoleOpt = mock(ILinkRoleOpt.class);
            when(polarionService.getLinkRoleById(any(), any())).thenReturn(linkRoleOpt);
            when(mergeService.getWorkItem(rightItem1)).thenReturn(mock(IWorkItem.class));

            MergeResult result = mergeService.mergeWorkItems(mergeParams);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            verify(polarionService).userAuthorizedForMerge("projectId");
        }
    }

    @Test
    void testMergeWorkItemsUnauthorizedUser() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DiffModel diffModel = mock(DiffModel.class);
        try (MockedStatic<DiffModelCachedResource> mockModelCache = mockStatic(DiffModelCachedResource.class)) {
            mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), anyString())).thenReturn(diffModel);
            WorkItemsMergeParams mergeParams = mock(WorkItemsMergeParams.class);
            Project leftProject = mock(Project.class);
            Project rightProject = mock(Project.class);
            when(mergeParams.getLeftProject()).thenReturn(leftProject);
            when(mergeParams.getRightProject()).thenReturn(rightProject);
            when(mergeParams.getDirection()).thenReturn(MergeDirection.LEFT_TO_RIGHT);
            when(mergeParams.getLinkRole()).thenReturn("reuse");
            when(mergeParams.getConfigName()).thenReturn("config");
            when(mergeParams.getConfigCacheBucketId()).thenReturn("ConfigCacheBucketId");
            when(leftProject.getId()).thenReturn("projectId");
            when(rightProject.getId()).thenReturn("projectId");

            when(polarionService.userAuthorizedForMerge("projectId")).thenReturn(false);

            MergeResult result = mergeService.mergeWorkItems(mergeParams);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertTrue(result.isMergeNotAuthorized());
            verify(polarionService).userAuthorizedForMerge("projectId");
        }
    }

    @Test
    void testUpdateItemWhenTargetHasChangedShouldReportConflict() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        WorkItemsMergeContext context = mock(WorkItemsMergeContext.class);

        WorkItem sourceWorkItem = mock(WorkItem.class);
        WorkItem targetWorkItem = mock(WorkItem.class);

        WorkItemsPair pair = mock(WorkItemsPair.class);

        when(context.getSourceWorkItem(pair)).thenReturn(sourceWorkItem);
        when(context.getTargetWorkItem(pair)).thenReturn(targetWorkItem);

        IWorkItem iWorkItem = mock(IWorkItem.class);
        when(iWorkItem.getLastRevision()).thenReturn("new-revision");
        when(mergeService.getWorkItem(context.getTargetWorkItem(pair))).thenReturn(iWorkItem);

        when(targetWorkItem.getLastRevision()).thenReturn("new-revision");
        when(context.getTargetWorkItem(pair).getLastRevision()).thenReturn("old-revision");

        mergeService.updateItem(pair, context, true);

        verify(context).reportEntry(eq(CONFLICTED), eq(pair), anyString());
        verify(context, never()).reportEntry(eq(MODIFIED), eq(pair), anyString());
    }

    @Test
    void testUpdateItemWhenTargetNotChanged_ShouldMergeAndReportModification() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        WorkItemsMergeContext context = new WorkItemsMergeContext(mock(Project.class), mock(Project.class), MergeDirection.LEFT_TO_RIGHT, "", mock(DiffModel.class));

        WorkItem sourceWorkItem = mock(WorkItem.class);
        WorkItem targetWorkItem = mock(WorkItem.class);

        WorkItemsPair pair = mock(WorkItemsPair.class);

        when(context.getSourceWorkItem(pair)).thenReturn(sourceWorkItem);
        when(context.getTargetWorkItem(pair)).thenReturn(targetWorkItem);

        IWorkItem iWorkItem = mock(IWorkItem.class);
        when(iWorkItem.getLastRevision()).thenReturn("same-revision");
        context.putTargetWorkItem(pair, iWorkItem);
        when(mergeService.getWorkItem(context.getTargetWorkItem(pair))).thenReturn(iWorkItem);

        when(targetWorkItem.getLastRevision()).thenReturn("same-revision");
        when(context.getTargetWorkItem(pair).getLastRevision()).thenReturn("same-revision");
        WorkItemsMergeContext mergeContext = spy(context);
        mergeService.updateItem(pair, context, true);
        assertNotNull(mergeContext.getMergeReport().getEntriesByType(MODIFIED));
    }

    @Test
    void testInsertWorkItemSameProject() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem destinationParentWorkItem = mock(IWorkItem.class);
        IModule targetModule = mock(IModule.class);
        IModule sourceModule = mock(IModule.class);
        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode sourceParentNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode destinationParentNode = mock(IModule.IStructureNode.class);
        DocumentsMergeContext context = mock(DocumentsMergeContext.class);

        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);

        when(sourceWorkItem.getProjectId()).thenReturn("projectA");
        when(targetModule.getProjectId()).thenReturn("projectB");

        when(polarionService.getPairedWorkItems(any(), any(), any())).thenReturn(List.of(destinationParentWorkItem));

        when(sourceModule.getStructureNodeOfWI(sourceWorkItem)).thenReturn(sourceNode);
        when(sourceNode.getParent()).thenReturn(sourceParentNode);
        when(sourceParentNode.getWorkItem()).thenReturn(sourceWorkItem);

        when(targetModule.getStructureNodeOfWI(destinationParentWorkItem)).thenReturn(destinationParentNode);

        when(sourceWorkItem.getProjectId()).thenReturn("projectB");

        IWorkItem result = mergeService.insertWorkItem(sourceWorkItem, context, true);

        assertEquals(sourceWorkItem, result);
        verify(polarionService, times(1)).insertWorkItem(eq(sourceWorkItem), eq(targetModule), any(), anyInt(), eq(true));
    }

    @Test
    void testInsertWorkItemDifferentProjectWithPairedItem() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        IModule targetModule = mock(IModule.class);
        IModule sourceModule = mock(IModule.class);
        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode sourceParentNode = mock(IModule.IStructureNode.class);

        DocumentsMergeContext context = mock(DocumentsMergeContext.class);

        when(sourceModule.getStructureNodeOfWI(sourceWorkItem)).thenReturn(sourceNode);
        when(sourceNode.getParent()).thenReturn(sourceParentNode);
        when(sourceParentNode.getWorkItem()).thenReturn(sourceWorkItem);
        when(sourceParentNode.getChildren()).thenReturn(List.of(sourceNode));

        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);

        when(sourceWorkItem.getProjectId()).thenReturn("projectA");
        when(targetModule.getProjectId()).thenReturn("projectB");

        when(polarionService.getPairedWorkItems(any(), any(), any()))
                .thenReturn(List.of(pairedWorkItem));

        when(sourceParentNode.getChildren()).thenReturn(List.of(sourceNode));

        when(polarionService.getPairedWorkItems(eq(sourceParentNode.getWorkItem()), eq("projectB"), any()))
                .thenReturn(List.of(pairedWorkItem));

        IWorkItem result = mergeService.insertWorkItem(sourceWorkItem, context, false);

        assertEquals(pairedWorkItem, result);
        verify(polarionService, times(2)).getPairedWorkItems(sourceWorkItem, "projectB", null);
    }

    @Test
    void testCreateReferencedWorkItemWhenTargetIsNull() {
        WorkItemsPair pair = mock(WorkItemsPair.class);
        IWorkItem iWorkItem = mock(IWorkItem.class);
        when(iWorkItem.getProjectId()).thenReturn("projectA");

        WorkItem source = mock(WorkItem.class);
        WorkItem target = mock(WorkItem.class);

        DocumentsMergeContext context = mock(DocumentsMergeContext.class);
        when(context.getSourceWorkItem(pair)).thenReturn(source);
        when(context.getTargetWorkItem(pair)).thenReturn(target);

        when(source.getId()).thenReturn("sourceId");

        IModule sourceModule = mock(IModule.class);
        IModule targetModule = mock(IModule.class);
        when(targetModule.getProjectId()).thenReturn("projectA");
        when(context.getTargetModule()).thenReturn(targetModule);

        IModule.IStructureNode destinationParentNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode sourceParentNode = mock(IModule.IStructureNode.class);
        when(sourceModule.getStructureNodeOfWI(iWorkItem)).thenReturn(destinationParentNode);
        when(destinationParentNode.getParent()).thenReturn(sourceParentNode);
        when(sourceParentNode.getWorkItem()).thenReturn(iWorkItem);

        when(polarionService.getPairedWorkItems(iWorkItem, "projectA", null)).thenReturn(List.of(iWorkItem));

        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);

        // Ensure there is no reason to create a new work item
        when(sourceModule.getExternalWorkItems()).thenReturn(List.of(iWorkItem));

        MergeService service = spy(mergeService);
        when(service.getWorkItem(source)).thenReturn(iWorkItem);
        doNothing().when(service).reloadModule(targetModule);
        boolean result = service.createOrDeleteItem(pair, context, mock(WriteTransaction.class));
        assertTrue(result);
    }

    @Test
    void testCreateWorkItemWhenTargetIsNull() {
        WorkItemsPair pair = mock(WorkItemsPair.class);
        IWorkItem iWorkItem = mock(IWorkItem.class);
        when(iWorkItem.getProjectId()).thenReturn("projectA");

        WorkItem source = mock(WorkItem.class);
        WorkItem target = mock(WorkItem.class);

        DocumentsMergeContext context = mock(DocumentsMergeContext.class);
        when(context.getSourceWorkItem(pair)).thenReturn(source);
        when(context.getTargetWorkItem(pair)).thenReturn(target);

        when(source.getId()).thenReturn("sourceId");

        IModule sourceModule = mock(IModule.class);
        IModule targetModule = mock(IModule.class);

        when(targetModule.getProjectId()).thenReturn("projectA");
        when(context.getTargetModule()).thenReturn(targetModule);

        IModule.IStructureNode destinationParentNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode sourceParentNode = mock(IModule.IStructureNode.class);
        when(sourceModule.getStructureNodeOfWI(iWorkItem)).thenReturn(destinationParentNode);
        when(destinationParentNode.getParent()).thenReturn(sourceParentNode);
        when(sourceParentNode.getWorkItem()).thenReturn(iWorkItem);

        when(polarionService.getPairedWorkItems(iWorkItem, "projectA", null)).thenReturn(List.of(iWorkItem));

        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);

        MergeService service = spy(mergeService);
        when(service.getWorkItem(source)).thenReturn(iWorkItem);
        doNothing().when(service).reloadModule(targetModule);
        boolean result = service.createOrDeleteItem(pair, context, mock(WriteTransaction.class));

        assertTrue(result);
        verify(context).reportEntry(eq(CREATED), eq(pair), anyString());
    }

    @Test
    void testCreateWorkItemWhenTargetIsNullAndInsertFailed() {
        WorkItemsPair pair = mock(WorkItemsPair.class);
        IWorkItem iWorkItem = mock(IWorkItem.class);
        when(iWorkItem.getProjectId()).thenReturn("projectA");
        when(iWorkItem.getId()).thenReturn("workItemId");

        WorkItem source = mock(WorkItem.class);
        WorkItem target = mock(WorkItem.class);

        DocumentsMergeContext context = mock(DocumentsMergeContext.class);
        when(context.getSourceWorkItem(pair)).thenReturn(source);
        when(context.getTargetWorkItem(pair)).thenReturn(target);

        when(source.getId()).thenReturn("sourceId");

        IModule sourceModule = mock(IModule.class);
        IModule targetModule = mock(IModule.class);
        when(targetModule.getProjectId()).thenReturn("projectA");
        when(context.getTargetModule()).thenReturn(targetModule);

        IModule.IStructureNode destinationParentNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode sourceParentNode = mock(IModule.IStructureNode.class);
        when(sourceModule.getStructureNodeOfWI(iWorkItem)).thenReturn(destinationParentNode);
        when(destinationParentNode.getParent()).thenReturn(sourceParentNode);
        when(sourceParentNode.getWorkItem()).thenReturn(iWorkItem);

        when(polarionService.getPairedWorkItems(iWorkItem, "projectA", null)).thenReturn(List.of(iWorkItem));

        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);

        MergeService service = spy(mergeService);
        when(service.getWorkItem(source)).thenReturn(iWorkItem);
        when(service.insertWorkItem(iWorkItem, context, false)).thenReturn(null);
        doNothing().when(service).reloadModule(targetModule);
        boolean result = service.createOrDeleteItem(pair, context, mock(WriteTransaction.class));

        verify(context).reportEntry(eq(CREATION_FAILED), eq(pair), anyString());
        assertTrue(result);
    }

    @Test
    void testCopyWorkItemWhenTargetIsNull() {
        when(polarionService.getStandardFields()).thenReturn(List.of(
                WorkItemField.builder().key("title").build(),
                WorkItemField.builder().key("status").build(),
                WorkItemField.builder().key("priority").build()
        ));
        when(polarionService.getDeletableFields(any(), any(), any())).thenCallRealMethod();

        WorkItemsPair pair = mock(WorkItemsPair.class);
        IWorkItem iWorkItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);

        WorkItem source = mock(WorkItem.class);
        WorkItem target = mock(WorkItem.class);

        DocumentsMergeContext context = mock(DocumentsMergeContext.class, RETURNS_DEEP_STUBS);
        when(context.getSourceWorkItem(pair)).thenReturn(source);
        when(context.getTargetWorkItem(pair)).thenReturn(target);

        DiffModel diffModel = mock(DiffModel.class);
        when(diffModel.getDiffFields()).thenReturn(List.of(
                DiffField.builder().key("title").build(),
                DiffField.builder().key("status").build()
        ));
        when(context.getDiffModel()).thenReturn(diffModel);

        when(source.getId()).thenReturn("sourceId");

        IModule sourceModule = mock(IModule.class);
        IModule targetModule = mock(IModule.class);
        when(targetModule.getProjectId()).thenReturn("projectA");
        ITrackerProject targetProjectMock = mock(ITrackerProject.class);
        when(targetProjectMock.getContextId()).thenReturn(mock(IContextId.class));
        when(targetModule.getProject()).thenReturn(targetProjectMock);
        when(context.getTargetModule()).thenReturn(targetModule);

        when(polarionService.getPairedWorkItems(iWorkItem, "projectA", null)).thenReturn(List.of());

        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);

        Text text = mock(Text.class);
        when(text.getContent()).thenReturn("""
                <html>
                    <h3 id="123">Test</h3>
                    <p>Test text</p>
                    <div id="456">test</div>
                    <p>Test Text</p>
                </html>/>
                """);
        when(context.getSourceModule().getHomePageContent()).thenReturn(text);
        when(context.getTargetModule().getHomePageContent()).thenReturn(text);

        when(sourceModule.getExternalWorkItems()).thenReturn(List.of());

        MergeService service = spy(mergeService);
        when(service.getWorkItem(source)).thenReturn(iWorkItem);
        doNothing().when(service).reloadModule(targetModule);

        InternalWriteTransaction internalWriteTransaction = mock(InternalWriteTransaction.class);
        InternalUpdatableDocument internalUpdatableDocument = mock(InternalUpdatableDocument.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(internalUpdatableDocument.getReferenceToCurrent()).thenReturn(documentReference);
        when(documentReference.projectId()).thenReturn("projectA");

        DleWIsMergeActionExecuter dleWIsMergeActionExecuter = new DleWIsMergeActionExecuter(internalUpdatableDocument, mock(StrictList.class), mock(StrictList.class));
        WorkItemReference createdWorkItemReference = mock(WorkItemReference.class);
        when(createdWorkItemReference.projectId()).thenReturn("projectA");
        when(createdWorkItemReference.id()).thenReturn("id");
        dleWIsMergeActionExecuter.workItemsCreatedByDuplicateAction.add(createdWorkItemReference);
        doReturn(dleWIsMergeActionExecuter).when(service).getDleWIsMergeActionExecuter(any(), eq(internalWriteTransaction), eq(context));

        IListType iListType = mock(IListType.class, RETURNS_DEEP_STUBS);
        when(iWorkItem.getPrototype().getKeyType("linkedWorkItems")).thenReturn(iListType);
        IStructType istructType = mock(IStructType.class, RETURNS_DEEP_STUBS);
        when(iListType.getItemType()).thenReturn(istructType);
        IEnumType iEnumType = mock(IEnumType.class);
        when(iEnumType.getEnumerationId()).thenReturn("roleEnumId");
        when(istructType.getKeyType("role")).thenReturn(iEnumType);

        IInternalWorkItem createdWorkItem = mock(IInternalWorkItem.class);
        when(createdWorkItem.isHeading()).thenReturn(true);
        lenient().when(polarionService.getWorkItem("projectA", "id")).thenReturn(createdWorkItem);

        boolean result = service.createOrDeleteItem(pair, context, internalWriteTransaction);
        assertTrue(result);
    }

    @Test
    void testDeleteWorkItemWhenSourceIsNull() {
        WorkItem source = null;
        WorkItem target = mock(WorkItem.class);
        IWorkItem targetWorkItem = mock(IWorkItem.class);
        MergeService service = spy(mergeService);
        when(service.getWorkItem(null)).thenReturn(null);
        when(service.getWorkItem(target)).thenReturn(targetWorkItem);
        when(targetWorkItem.getId()).thenReturn("targetID");

        WorkItemsPair pair = mock(WorkItemsPair.class);
        when(pair.getLeftWorkItem()).thenReturn(source);
        when(pair.getRightWorkItem()).thenReturn(target);

        DocumentIdentifier leftIdentifier = mock(DocumentIdentifier.class);
        when(leftIdentifier.getProjectId()).thenReturn("projectA");
        DocumentIdentifier rightIdentifier = mock(DocumentIdentifier.class);
        when(rightIdentifier.getProjectId()).thenReturn("projectA");

        IModule leftModule = mock(IModule.class);
        ILocation leftLocation = mock(ILocation.class);
        when(leftLocation.getLocationPath()).thenReturn("test/leftLocation");
        when(leftModule.getModuleLocation()).thenReturn(leftLocation);

        IModule rightModule = mock(IModule.class);
        when(rightModule.getModuleLocation()).thenReturn(leftLocation);

        when(polarionService.getModule(leftIdentifier)).thenReturn(leftModule);
        when(polarionService.getModule(rightIdentifier)).thenReturn(rightModule);

        DiffModel model = mock(DiffModel.class);
        ILinkRoleOpt iLinkRoleOpt = mock(ILinkRoleOpt.class);
        when(polarionService.getLinkRoleById(eq("someLinkRole"), any())).thenReturn(iLinkRoleOpt);
        DocumentsMergeContext context = new DocumentsMergeContext(polarionService, leftIdentifier, rightIdentifier, MergeDirection.LEFT_TO_RIGHT, "someLinkRole", model, true);
        IWorkItem workItem = mock(IWorkItem.class);
        lenient().when(mergeService.getWorkItem(target)).thenReturn(workItem);

        when(polarionService.getModule("projectA", null, null)).thenReturn(rightModule);
        doNothing().when(service).reloadModule(any());
        boolean result = service.createOrDeleteItem(pair, context, mock(WriteTransaction.class));

        assertTrue(result);
        verify(service).deleteWorkItemFromDocument(context.getTargetDocumentIdentifier(), targetWorkItem);
        assertFalse(context.getMergeReport().getEntriesByType(DELETED).isEmpty());
        verify(service).reloadModule(context.getTargetModule());
    }

    @Test
    void testCreateItemSuccess() {
        WorkItemsMergeContext context = mock(WorkItemsMergeContext.class);
        WorkItemsPair pair = mock(WorkItemsPair.class);
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem newWorkItem = mock(IWorkItem.class);

        WorkItem workItem = mock(WorkItem.class);

        when(context.getSourceWorkItem(pair)).thenReturn(workItem);
        when(context.getTargetWorkItem(pair)).thenReturn(null);
        when(mergeService.getWorkItem(workItem)).thenReturn(sourceWorkItem);
        when(sourceWorkItem.getType()).thenReturn(mock(ITypeOpt.class));
        when(sourceWorkItem.getType().getId()).thenReturn("defect");
        when(context.getTargetProject()).thenReturn(mock(Project.class));
        when(context.getTargetProject().getId()).thenReturn("targetProjectId");
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("targetProjectId")).thenReturn(trackerProject);
        when(trackerProject.createWorkItem("defect")).thenReturn(newWorkItem);
        when(newWorkItem.getId()).thenReturn("newItemId");
        when(sourceWorkItem.getId()).thenReturn("sourceItemId");

        ILinkRoleOpt iLinkRoleOpt = mock(ILinkRoleOpt.class);
        IWorkItem result = mergeService.createItem(pair, iLinkRoleOpt, context);

        assertNotNull(result);
        verify(newWorkItem).addLinkedItem(sourceWorkItem, iLinkRoleOpt, null, false);
        verify(newWorkItem).save();
    }

    @Test
    void testDeleteItemSuccess() {
        WorkItemsMergeContext context = mock(WorkItemsMergeContext.class);
        WorkItemsPair pair = mock(WorkItemsPair.class);
        WorkItem targetWorkItem = mock(WorkItem.class);
        when(context.getSourceWorkItem(pair)).thenReturn(null);
        when(context.getTargetWorkItem(pair)).thenReturn(targetWorkItem);

        IWorkItem target = mock(IWorkItem.class);
        when(mergeService.getWorkItem(context.getTargetWorkItem(pair))).thenReturn(target);
        doNothing().when(target).delete();

        boolean result = mergeService.deleteItem(pair, context);

        assertTrue(result);
    }

    @Test
    void testMergeDocumentsFieldsSuccessful() {
        IModule source = mock(IModule.class);
        IModule target = mock(IModule.class);
        when(polarionService.getModule(any())).thenReturn(source, target);

        when(polarionService.userAuthorizedForMerge(any())).thenReturn(false);
        MergeResult mergeResult = mergeService.mergeDocumentsFields(new DocumentsFieldsMergeParams(mock(DocumentIdentifier.class), mock(DocumentIdentifier.class),
                MergeDirection.LEFT_TO_RIGHT, List.of("a", "b", "c")));
        assertFalse(mergeResult.isSuccess());
        assertTrue(mergeResult.isMergeNotAuthorized());
        try (MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(arg -> {
                RunnableInWriteTransaction<?> runnable = arg.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });
            when(polarionService.userAuthorizedForMerge(any())).thenReturn(true);

            mergeResult = mergeService.mergeDocumentsFields(new DocumentsFieldsMergeParams(mock(DocumentIdentifier.class), mock(DocumentIdentifier.class),
                    MergeDirection.LEFT_TO_RIGHT, List.of("a", "b", "c")));
            assertTrue(mergeResult.isSuccess());
            assertEquals(3, mergeResult.getMergeReport().getCopied().size());
        }
    }

    @Test
    void testMergeDocumentsFieldsUnsuccessful() {
        IModule source = mock(IModule.class);
        IModule target = mock(IModule.class);
        doThrow(IllegalArgumentException.class).when(target).setValue(anyString(), any());

        when(polarionService.getModule(any())).thenReturn(source, target);

        when(polarionService.userAuthorizedForMerge(any())).thenReturn(false);
        MergeResult mergeResult = mergeService.mergeDocumentsFields(new DocumentsFieldsMergeParams(mock(DocumentIdentifier.class), mock(DocumentIdentifier.class),
                MergeDirection.LEFT_TO_RIGHT, List.of("a", "b")));
        assertFalse(mergeResult.isSuccess());
        assertTrue(mergeResult.isMergeNotAuthorized());
        try (MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(arg -> {
                RunnableInWriteTransaction<?> runnable = arg.getArgument(0);
                runnable.run(mock(WriteTransaction.class));
                return runnable;
            });
            when(polarionService.userAuthorizedForMerge(any())).thenReturn(true);

            mergeResult = mergeService.mergeDocumentsFields(new DocumentsFieldsMergeParams(mock(DocumentIdentifier.class), mock(DocumentIdentifier.class),
                    MergeDirection.LEFT_TO_RIGHT, List.of("a", "b")));
            assertFalse(mergeResult.isSuccess());
            assertEquals(2, mergeResult.getMergeReport().getWarnings().size());
        }
    }

    private static Stream<Arguments> testValuesForMergeLinkedWorkItems() {
        ILinkedWorkItemStruct a1rev1role1 = mockLink("projA", "A-1", "1", "role1", ParentModule.SRC);
        ILinkedWorkItemStruct a1rev1role2 = mockLink("projA", "A-1", "2", "role2", ParentModule.SRC);
        ILinkedWorkItemStruct a1rev2role1 = mockLink("projA", "A-1", "2", "role1", ParentModule.SRC);
        ILinkedWorkItemStruct b1rev1role1 = mockLink("projB", "B-1", "1", "role1", ParentModule.SRC);
        ILinkedWorkItemStruct t1rev1role1 = mockLink("projB", "T-1", "1", "role1", ParentModule.TARGET);
        ILinkedWorkItemStruct s1rev1role1 = mockLink("projA", "S-1", "1", "role1", ParentModule.SRC);
        ILinkedWorkItemStruct c1rev1role1 = mockLink("projC", "C-1", "1", "role1", ParentModule.OTHER);
        return Stream.of(
                // nothing to merge
                Arguments.of(sameProject(), linksList(), linksList(), List.of(), List.of()),

                // link without source pair will be removed
                Arguments.of(sameProject(), linksList(), linksList(a1rev1role1), List.of(), List.of()),

                // now (when same src link exist) initial link will be left in place
                Arguments.of(sameProject(), linksList(a1rev1role1), linksList(a1rev1role1), List.of(a1rev1role1), List.of()),

                // role must be the same
                Arguments.of(sameProject(), linksList(a1rev1role2), linksList(a1rev1role1), List.of(), List.of()),

                // link project must be the same too
                Arguments.of(anotherProject(null), linksList(a1rev1role1), linksList(b1rev1role1), List.of(), List.of()),

                // src has the same but another revision link - target link removed and added a new one
                Arguments.of(sameProject(), linksList(a1rev2role1, a1rev1role2), linksList(a1rev1role1), List.of(),
                        List.of(new AddLinkedItemInvocation("projA", "A-1", "2", "role1"))),

                // interlinked items - link stays at its place
                Arguments.of(anotherProject(null), linksList(t1rev1role1), linksList(s1rev1role1), List.of(s1rev1role1), List.of()),

                // link to another project - copy it
                Arguments.of(anotherProject(null), linksList(c1rev1role1), linksList(), List.of(), List.of(new AddLinkedItemInvocation("projC", "C-1", "1", "role1"))),

                // store link to paired work item from target project
                Arguments.of(anotherProject(polarionService -> {
                            IWorkItem oppositeWorkItem = mockWorkItem("projB", "B-1", "3", null, null);
                            lenient().when(polarionService.getPairedWorkItems(any(), anyString(), anyString())).thenReturn(List.of(oppositeWorkItem));
                        }),
                        linksList(a1rev1role1), linksList(), List.of(), List.of(new AddLinkedItemInvocation("projB", "B-1", "3", "role1")))
        );
    }

    @ParameterizedTest
    @MethodSource("testValuesForMergeLinkedWorkItems")
    void testMergeLinkedWorkItems(MergeTestContext testContext,
                                  List<ILinkedWorkItemStruct> sourceList, List<ILinkedWorkItemStruct> targetList,
                                  List<ILinkedWorkItemStruct> resultTargetList, List<AddLinkedItemInvocation> addLinkedItemInvocations) {
        IWorkItem source = mockWorkItem(testContext.srcProjectId, "S-1", "1", testContext.srcModule, sourceList);
        IWorkItem target = mockWorkItem(testContext.targetProjectId, "T-1", "1", testContext.targetModule, targetList);

        for (ILinkedWorkItemStruct item : sourceList) {
            switch (((ParentModule) item.getValue("parentModule"))) {
                case SRC -> when(item.getLinkedItem().getModule()).thenReturn(testContext.srcModule());
                case TARGET -> when(item.getLinkedItem().getModule()).thenReturn(testContext.targetModule());
                case OTHER -> when(item.getLinkedItem().getModule()).thenReturn(testContext.otherModule());
                case NONE -> when(item.getLinkedItem().getModule()).thenReturn(null
                );
            }
        }

        if (testContext.polarionServiceConsumer != null) {
            testContext.polarionServiceConsumer.accept(polarionService);
        }

        SettingsAwareMergeContext context = mock(SettingsAwareMergeContext.class);
        lenient().when(context.getLinkRole()).thenReturn("role1");
        lenient().when(context.getDiffModel()).thenReturn(DiffModel.builder().linkedWorkItemRoles(List.of("role1")).build());

        mergeService.mergeLinkedWorkItems(source, target, context, new WorkItemsPair());

        if (resultTargetList.isEmpty()) {
            assertTrue(targetList.isEmpty());
        } else {
            assertTrue(targetList.size() == resultTargetList.size() && targetList.containsAll(resultTargetList));
        }

        if (addLinkedItemInvocations.isEmpty()) {
            verify(target, times(0)).addLinkedItem(any(), any(), anyString(), anyBoolean());
        } else {
            for (AddLinkedItemInvocation invocation : addLinkedItemInvocations) {
                verify(target, times(1)).addLinkedItem(
                        argThat(arg -> arg.getProjectId().equals(invocation.projectId) && arg.getId().equals(invocation.workItemId)),
                        argThat(arg -> arg.getId().equals(invocation.linkRole)),
                        eq(invocation.linkRevision), anyBoolean());
            }
        }
    }

    @Test
    void testMoveWhenTargetDestinationNodeExists() {
        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode targetNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode targetDestinationParentNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode targetDestinationNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode parent = mock(IModule.IStructureNode.class, RETURNS_DEEP_STUBS);
        DocumentsMergeContext mergeContext = mock(DocumentsMergeContext.class);
        IModule targetModule = mock(IModule.class);
        IWorkItem targetWorkItem = mock(IWorkItem.class);
        when(targetModule.getAllWorkItems()).thenReturn(List.of(targetWorkItem));
        when(targetModule.getOutlineNumberOfWorkitem(targetWorkItem)).thenReturn("1");

        when(mergeContext.getTargetModule()).thenReturn(targetModule);

        List<IModule.IStructureNode> children = new ArrayList<>();
        children.add(targetNode);
        children.add(mock(IModule.IStructureNode.class));
        when(parent.getChildren()).thenReturn(children);
        when(targetDestinationNode.getParent()).thenReturn(parent);

        MergeService service = spy(mergeService);
        when(service.getTargetDestinationNumber(sourceNode, targetDestinationParentNode)).thenReturn("1");
        when(service.getNodeByOutlineNumber(targetModule, "1")).thenReturn(targetDestinationNode);


        boolean result = service.move(sourceNode, targetNode, targetDestinationParentNode, mergeContext);

        assertTrue(result);
        verify(parent, times(1)).addChild(any(), anyInt());
    }

    @Test
    void testMoveWhenTargetDestinationNodeDoesNotExist() {
        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode targetNode = mock(IModule.IStructureNode.class);

        IModule.IStructureNode targetDestinationParentNode = mock(IModule.IStructureNode.class);

        IModule.IStructureNode parent = mock(IModule.IStructureNode.class, RETURNS_DEEP_STUBS);
        DocumentsMergeContext mergeContext = mock(DocumentsMergeContext.class);
        IModule targetModule = mock(IModule.class);
        IWorkItem targetWorkItem = mock(IWorkItem.class);
        when(targetModule.getAllWorkItems()).thenReturn(List.of(targetWorkItem));
        when(targetModule.getOutlineNumberOfWorkitem(targetWorkItem)).thenReturn("1");

        when(mergeContext.getTargetModule()).thenReturn(targetModule);

        List<IModule.IStructureNode> children = new ArrayList<>();
        children.add(targetNode);
        children.add(mock(IModule.IStructureNode.class));
        when(parent.getChildren()).thenReturn(children);

        MergeService service = spy(mergeService);
        when(service.getTargetDestinationNumber(sourceNode, targetDestinationParentNode)).thenReturn("1");
        when(service.getNodeByOutlineNumber(targetModule, "1")).thenReturn(null);


        boolean result = service.move(sourceNode, targetNode, targetDestinationParentNode, mergeContext);

        assertTrue(result);
        verify(targetDestinationParentNode, times(1)).moveNodeAsLastChild(targetNode);
    }

    @Test
    void testRoleNotFound() {
        PolarionService polarionService = mock(PolarionService.class);

        DocumentIdentifier leftIdentifier = mock(DocumentIdentifier.class);
        DocumentIdentifier rightIdentifier = mock(DocumentIdentifier.class);

        IModule leftModule = mock(IModule.class);
        IModule rightModule = mock(IModule.class);
        when(polarionService.getModule(leftIdentifier)).thenReturn(leftModule);
        when(polarionService.getModule(rightIdentifier)).thenReturn(rightModule);

        DiffModel model = mock(DiffModel.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new DocumentsMergeContext(polarionService, leftIdentifier, rightIdentifier, MergeDirection.LEFT_TO_RIGHT, "someLinkRole", model, true));
        assertEquals("No link role could be found by ID 'someLinkRole'", exception.getMessage());
    }

    @Test
    void testGetDleWIsMergeActionExecuter() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        InternalWriteTransaction transaction = mock(InternalWriteTransaction.class);
        DocumentsMergeContext context = mock(DocumentsMergeContext.class);
        InternalDocument leftDocument = mock(InternalDocument.class);
        InternalDocument rightDocument = mock(InternalDocument.class);
        DleWorkItemsComparator comparator = mock(DleWorkItemsComparator.class);
        InternalUpdatableDocument targetDocument = mock(InternalUpdatableDocument.class);
        DleWIsMergeAction action = mock(DleWIsMergeAction.class);

        DocumentReference mockSourceDocumentReference = mock(DocumentReference.class);
        DocumentReference mockTargetDocumentReference = mock(DocumentReference.class);

        when(targetDocument.getReferenceToCurrent()).thenReturn(mockTargetDocumentReference);

        when(context.getSourceDocumentReference()).thenReturn(mockSourceDocumentReference);
        when(context.getTargetDocumentReference()).thenReturn(mockTargetDocumentReference);

        when(mockSourceDocumentReference.get(transaction)).thenReturn(leftDocument);
        when(mockTargetDocumentReference.get(transaction)).thenReturn(rightDocument);

        MergeService service = spy(mergeService);

        doReturn(comparator).when(service).createComparator(transaction, leftDocument, rightDocument);

        when(context.getTargetDocumentReference().getUpdatable(transaction)).thenReturn(targetDocument);
        when(comparator.getLeftMergedPartsOrder()).thenReturn(mock(StrictList.class));
        when(comparator.getRightMergedPartsOrder()).thenReturn(mock(StrictList.class));
        when(sourceWorkItem.getProjectId()).thenReturn("projectId");
        when(sourceWorkItem.getId()).thenReturn("workItemId");
        when(sourceWorkItem.getRevision()).thenReturn("revision");
        when(service.getAction(sourceWorkItem)).thenReturn(action);
        doReturn(mock(Consumer.class)).when(action).execute(any());

        DleWIsMergeActionExecuter result = service.getDleWIsMergeActionExecuter(sourceWorkItem, transaction, context);

        assertNotNull(result);
        assertInstanceOf(DleWIsMergeActionExecuter.class, result);
    }

    @Test
    void testRemoveWrongHyperlinks() {
        DocumentsMergeContext context = mock(DocumentsMergeContext.class);
        DiffModel diffModel = mock(DiffModel.class);
        when(context.getDiffModel()).thenReturn(diffModel);
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemsPair pair = mock(WorkItemsPair.class);

        HyperlinkStruct allowedLink = mock(HyperlinkStruct.class);
        IHyperlinkRoleOpt iHyperlinkRoleOpt = mock(IHyperlinkRoleOpt.class);
        when(iHyperlinkRoleOpt.getId()).thenReturn("role");
        when(allowedLink.getRole()).thenReturn(iHyperlinkRoleOpt);

        HyperlinkStruct forbiddenLink = mock(HyperlinkStruct.class);
        IHyperlinkRoleOpt forbiddenHyperlinkRoleOpt = mock(IHyperlinkRoleOpt.class);
        when(forbiddenHyperlinkRoleOpt.getId()).thenReturn("forbiddenHyperlinkRole");
        when(forbiddenLink.getRole()).thenReturn(forbiddenHyperlinkRoleOpt);

        HyperlinkStruct wrongConfigLink = mock(HyperlinkStruct.class);
        IHyperlinkRoleOpt wrongHyperlinkRoleOpt = mock(IHyperlinkRoleOpt.class);
        when(wrongHyperlinkRoleOpt.getId()).thenReturn("wrongHyperlinkRole");
        when(wrongConfigLink.getRole()).thenReturn(wrongHyperlinkRoleOpt);

        List<Object> hyperlinks = new ArrayList<>();
        hyperlinks.add(allowedLink);
        hyperlinks.add(forbiddenLink);
        hyperlinks.add(wrongConfigLink);

        when(workItem.getHyperlinks()).thenReturn(hyperlinks);
        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("type");
        when(workItem.getType()).thenReturn(typeOpt);

        when(workItem.getProjectId()).thenReturn("projectId");
        LinkRole linkRole = mock(LinkRole.class);
        when(linkRole.getId()).thenReturn("role");
        when(linkRole.getWorkItemTypeId()).thenReturn("type");
        when(polarionService.getHyperlinkRoles("projectId")).thenReturn(List.of(linkRole));

        when(diffModel.getHyperlinkRoles()).thenReturn(List.of("", "type#role", "type#wrongHyperlinkRole"));
        when(context.getDiffModel()).thenReturn(diffModel);

        mergeService.removeWrongHyperlinks(workItem, context, pair);

        assertTrue(hyperlinks.contains(allowedLink));
        assertFalse(hyperlinks.contains(forbiddenLink));
        assertFalse(hyperlinks.contains(wrongConfigLink));
        verify(context).reportEntry(eq(WARNING), eq(pair), anyString());
    }

    @Test
    void testMergeHyperlinksNewLinksAdded() {
        IWorkItem workItem = mock(IWorkItem.class);
        SettingsAwareMergeContext context = mock(SettingsAwareMergeContext.class);
        DiffModel diffModel = mock(DiffModel.class);

        when(context.getDiffModel()).thenReturn(diffModel);
        when(diffModel.getHyperlinkRoles()).thenReturn(List.of("", "type#role"));

        WorkItemsPair pair = mock(WorkItemsPair.class);

        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("type");
        when(workItem.getType()).thenReturn(typeOpt);

        HyperlinkStruct existingLink = mock(HyperlinkStruct.class);
        IHyperlinkRoleOpt iHyperlinkRoleOpt = mock(IHyperlinkRoleOpt.class);
        when(iHyperlinkRoleOpt.getId()).thenReturn("role");
        when(existingLink.getRole()).thenReturn(iHyperlinkRoleOpt);

        HyperlinkStruct newLink = mock(HyperlinkStruct.class);
        when(newLink.getRole()).thenReturn(iHyperlinkRoleOpt);

        when(existingLink.getUri()).thenReturn("existing");
        when(newLink.getUri()).thenReturn("new");

        List<HyperlinkStruct> hyperlinks = new ArrayList<>();
        hyperlinks.add(existingLink);
        when(workItem.getHyperlinks()).thenReturn(hyperlinks);

        List<HyperlinkStruct> newLinksList = List.of(newLink);

        when(workItem.getProjectId()).thenReturn("projectId");
        LinkRole linkRole = mock(LinkRole.class);
        when(linkRole.getId()).thenReturn("role");
        when(linkRole.getWorkItemTypeId()).thenReturn("type");
        when(polarionService.getHyperlinkRoles("projectId")).thenReturn(List.of(linkRole));

        mergeService.mergeHyperlinks(workItem, newLinksList, context, pair);

        verify(workItem, times(1)).addHyperlink("new", newLink.getRole());
        assertFalse(workItem.getHyperlinks().contains(existingLink));
    }

    @Test
    void testInsertWorkItem() {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");

        DocumentsMergeContext context = mock(DocumentsMergeContext.class);

        IModule targetModule = mock(IModule.class);
        IModule sourceModule = mock(IModule.class);

        when(targetModule.getProjectId()).thenReturn("projectId");

        when(context.getSourceModule()).thenReturn(sourceModule);
        when(context.getTargetModule()).thenReturn(targetModule);

        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode sourceParentNode = mock(IModule.IStructureNode.class);
        when(sourceNode.getParent()).thenReturn(sourceParentNode);
        when(sourceModule.getStructureNodeOfWI(any())).thenReturn(sourceNode);

        ArrayList<DocumentsMergeContext.AffectedModule> affectedModules = new ArrayList<>();
        lenient().when(context.getAffectedModules()).thenReturn(affectedModules);

        mergeService.insertWorkItem(workItem, context, false);
        assertEquals(0, affectedModules.size());

        when(workItem.getModule()).thenReturn(targetModule);
        mergeService.insertWorkItem(workItem, context, false);
        assertEquals(0, affectedModules.size());

        when(workItem.getModule()).thenReturn(sourceModule);
        mergeService.insertWorkItem(workItem, context, false);
        assertEquals(1, affectedModules.size());
    }

    private static MergeTestContext sameProject() {
        return new MergeTestContext("projA", "projA", mockModule("projA"), mockModule("projB"), mockModule("projC"), null);
    }

    private static MergeTestContext anotherProject(Consumer<PolarionService> polarionServiceConsumer) {
        return new MergeTestContext("projA", "projB", mockModule("projA"), mockModule("projB"), mockModule("projC"), polarionServiceConsumer);
    }

    private enum ParentModule {SRC, TARGET, OTHER, NONE}

    private record MergeTestContext(String srcProjectId, String targetProjectId, IModule srcModule, IModule targetModule, IModule otherModule, Consumer<PolarionService> polarionServiceConsumer) {
    }

    private record AddLinkedItemInvocation(String projectId, String workItemId, String linkRevision, String linkRole) {
    }

    private static List<ILinkedWorkItemStruct> linksList(ILinkedWorkItemStruct... links) {
        return new ArrayList<>(List.of(links));
    }

    private static IModule mockModule(String projectId) {
        IModule module = mock(IModule.class);
        when(module.getProjectId()).thenReturn(projectId);
        ILocation location = mock(ILocation.class);
        when(module.getModuleLocation()).thenReturn(location);
        when(location.removeRevision()).thenReturn(location);
        return module;
    }

    private static ILinkedWorkItemStruct mockLink(String projectId, String workItemId, String linkRevision, String linkRole, ParentModule parentModule) {
        ILinkedWorkItemStruct linkMock = mock(ILinkedWorkItemStruct.class);
        IWorkItem workItemMock = mock(IWorkItem.class);
        lenient().when(workItemMock.getId()).thenReturn(workItemId);
        lenient().when(workItemMock.getProjectId()).thenReturn(projectId);
        lenient().when(linkMock.getLinkedItem()).thenReturn(workItemMock);
        lenient().when(linkMock.getRevision()).thenReturn(linkRevision);

        IModule module = mock(IModule.class);
        when(module.getProjectId()).thenReturn(projectId);
        when(workItemMock.getModule()).thenReturn(module);

        when(linkMock.getValue(eq("parentModule"))).thenReturn(parentModule);

        ILinkRoleOpt linkRoleOpt = mock(ILinkRoleOpt.class);
        lenient().when(linkRoleOpt.getId()).thenReturn(linkRole);
        lenient().when(linkMock.getLinkRole()).thenReturn(linkRoleOpt);
        return linkMock;
    }

    private static IWorkItem mockWorkItem(String projectId, String id, String revision, IModule module, Collection<ILinkedWorkItemStruct> links) {
        IWorkItem item = mock(IWorkItem.class);
        lenient().when(item.getId()).thenReturn(id);
        lenient().when(item.getProjectId()).thenReturn(projectId);
        lenient().when(item.getRevision()).thenReturn(revision);
        lenient().when(item.getModule()).thenReturn(module);
        lenient().when(item.getLinkedWorkItemsStructsDirect()).thenReturn(links);
        return item;
    }

}

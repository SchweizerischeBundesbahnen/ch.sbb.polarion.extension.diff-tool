package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.HandleReferencesType;
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
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.internal.model.HyperlinkStruct;
import com.polarion.alm.tracker.internal.model.IInternalWorkItem;
import com.polarion.alm.tracker.internal.model.TestStep;
import com.polarion.alm.tracker.internal.model.TestSteps;
import com.polarion.alm.tracker.model.IHyperlinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestStepKeyOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.internal.CustomFieldsService;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.CustomField;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IStructType;
import com.polarion.subterra.base.data.model.IType;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

        mockCustomFields();

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
        when(leftWorkItem.getPrototype()).thenReturn(mock(IPrototype.class));

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
        verify(service).fixReferencedWorkItem(rightWorkItem, rightModule, context, HandleReferencesType.DEFAULT);
    }

    @Test
    void testFixReferencedWorkItem() {
        MergeService service = spy(mergeService);
        doNothing().when(service).merge(any(), any(), any(), nullable(WorkItemsPair.class));
        doNothing().when(service).insertNode(any(), any(), any(), anyInt(), anyBoolean());

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(polarionService.getPairedWorkItems(any(), any(), any())).thenReturn(List.of(pairedWorkItem));

        IWorkItem referencedWorkItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
        when(referencedWorkItem.getProjectId()).thenReturn("srcProjectId");

        IModule targetModule = mock(IModule.class, RETURNS_DEEP_STUBS);
        when(targetModule.getProjectId()).thenReturn("targetProjectId");
        SettingsAwareMergeContext context = mock(SettingsAwareMergeContext.class);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("targetProjectId")).thenReturn(trackerProject);
        when(trackerProject.createWorkItem(any())).thenReturn(mock(IWorkItem.class));
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));

        when(context.getLinkRole()).thenReturn("linkRoleId");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.DEFAULT);
        verify(targetModule, times(1)).unreference(referencedWorkItem);
        verify(service, times(1)).insertNode(any(), any(), any(), anyInt(), eq(true));

        when(context.getLinkRole()).thenReturn("");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.DEFAULT);
        verify(targetModule, times(2)).unreference(referencedWorkItem);
        verify(service, times(1)).insertNode(any(), any(), any(), anyInt(), eq(true));

        when(context.getLinkRole()).thenReturn("linkRoleId");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.KEEP);
        verify(targetModule, times(3)).unreference(referencedWorkItem);
        verify(service, times(2)).insertNode(any(), any(), any(), anyInt(), eq(true));

        when(context.getLinkRole()).thenReturn("");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.KEEP);
        verify(targetModule, times(3)).unreference(referencedWorkItem);
        verify(service, times(2)).insertNode(any(), any(), any(), anyInt(), eq(true));

        when(context.getLinkRole()).thenReturn("linkRoleId");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.CREATE_MISSING);
        verify(targetModule, times(4)).unreference(referencedWorkItem);
        verify(service, times(0)).insertNode(any(), any(), any(), anyInt(), eq(false));
        verify(service, times(3)).insertNode(any(), any(), any(), anyInt(), eq(true));

        when(context.getLinkRole()).thenReturn("");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.CREATE_MISSING);
        verify(targetModule, times(5)).unreference(referencedWorkItem);
        verify(service, times(1)).insertNode(any(), any(), any(), anyInt(), eq(false));
        verify(service, times(3)).insertNode(any(), any(), any(), anyInt(), eq(true));

        when(context.getLinkRole()).thenReturn("linkRoleId");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.ALWAYS_OVERWRITE);
        verify(targetModule, times(6)).unreference(referencedWorkItem);
        verify(service, times(2)).insertNode(any(), any(), any(), anyInt(), eq(false));

        when(context.getLinkRole()).thenReturn("");
        service.fixReferencedWorkItem(referencedWorkItem, targetModule, context, HandleReferencesType.ALWAYS_OVERWRITE);
        verify(targetModule, times(7)).unreference(referencedWorkItem);
        verify(service, times(3)).insertNode(any(), any(), any(), anyInt(), eq(false));
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
        IModule targetModule = mockTargetModule();
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
    }

    @Test
    void testInsertWorkItemDifferentProjectWithPairedItem() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        IModule targetModule = mockTargetModule();
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
        IModule targetModule = mockTargetModule();
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
        IModule targetModule = mockTargetModule();

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
        IModule targetModule = mockTargetModule();
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
        mockCustomFields();

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
    void testInsertWorkItem() {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");

        DocumentsMergeContext context = mock(DocumentsMergeContext.class);

        IModule targetModule = mockTargetModule();
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

    @Test
    void testInsertNode() {
        IModule targetModule = mock(IModule.class);

        IDataService dataService = mock(IDataService.class);
        when(targetModule.getDataSvc()).thenReturn(dataService);

        IModule.IStructureNode rootNode = mock(IModule.IStructureNode.class);
        when(targetModule.getRootNode()).thenReturn(rootNode);

        IModule.IStructureNode parentRootNode = mock(IModule.IStructureNode.class);
        when(rootNode.getChildren()).thenReturn(List.of(parentRootNode));

        IModule.IStructureNode node = mock(IModule.IStructureNode.class);
        when(dataService.createStructureForTypeId(any(), anyString(), any())).thenReturn(node);

        IWorkItem workItem = mock(IWorkItem.class);

        mergeService.insertNode(workItem, targetModule, mock(IModule.IStructureNode.class), 1, false);
        verify(node, times(1)).setValue("workItem", workItem);
        verify(node, times(1)).setValue("external", false);

        mergeService.insertNode(workItem, targetModule, null, 1, true);
        verify(node, times(2)).setValue("workItem", workItem);
        verify(node, times(1)).setValue("external", true);

        IModule.IStructureNode structureNode = mock(IModule.IStructureNode.class);
        when(targetModule.getStructureNodeOfWI(workItem)).thenReturn(structureNode);
        mergeService.insertNode(workItem, targetModule, null, 5, true);
        verify(parentRootNode, times(1)).addChild(structureNode, 5);
    }

    @Test
    void testNotCorrespondingFieldTypesThrowsError() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));
        DocumentIdentifier doc1 = new DocumentIdentifier("project1", "space1", "doc1", "rev1", "rev1");
        DocumentIdentifier doc2 = new DocumentIdentifier("project2", "space2", "doc2", "rev1", "rev1");

        CustomField sourceCustomFieldMock = mock(CustomField.class);
        IType sourceTypeMock = mock(IType.class);
        when(sourceCustomFieldMock.getType()).thenReturn(sourceTypeMock);
        CustomField targetCustomFieldMock = mock(CustomField.class);
        IType targetTypeMock = mock(IType.class);
        when(targetCustomFieldMock.getType()).thenReturn(targetTypeMock);
        CustomFieldsService customFieldsServiceMock = mock(CustomFieldsService.class);
        when(customFieldsServiceMock.getCustomField(argThat(obj -> obj instanceof IWorkItem workItem && workItem.getProjectId().equals("project1")), any())).thenReturn(sourceCustomFieldMock);
        when(customFieldsServiceMock.getCustomField(argThat(obj -> obj instanceof IWorkItem workItem && workItem.getProjectId().equals("project2")), any())).thenReturn(targetCustomFieldMock);
        IDataService dataServiceMock = mock(IDataService.class);
        when(dataServiceMock.getCustomFieldsService()).thenReturn(customFieldsServiceMock);
        ITrackerService trackerServiceMock = mock(ITrackerService.class);
        when(trackerServiceMock.getDataService()).thenReturn(dataServiceMock);
        when(polarionService.getTrackerService()).thenReturn(trackerServiceMock);

        IModule leftModule = mock(IModule.class);
        ILocation leftLocation = mock(ILocation.class);
        when(leftLocation.getLocationPath()).thenReturn("space1/doc1");
        when(leftModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(leftModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc1)).thenReturn(leftModule);

        IModule rightModule = mock(IModule.class);
        ILocation rightLocation = mock(ILocation.class);
        when(rightLocation.getLocationPath()).thenReturn("space2/doc2");
        when(rightModule.getModuleLocation()).thenReturn(rightLocation);
        lenient().when(rightModule.getLastRevision()).thenReturn("rev1");
        when(polarionService.getModule(doc2)).thenReturn(rightModule);

        IWorkItem leftWorkItem = mock(IWorkItem.class);
        when(leftWorkItem.getId()).thenReturn("left");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getLastRevision()).thenReturn("rev1");
        when(leftWorkItem.getPrototype()).thenReturn(mock(IPrototype.class));

        IWorkItem rightWorkItem = mock(IWorkItem.class);
        when(rightWorkItem.getId()).thenReturn("right");
        when(rightWorkItem.getProjectId()).thenReturn("project2");
        when(rightWorkItem.getLastRevision()).thenReturn("rev1");

        List<WorkItemsPair> workItemsPairs = new ArrayList<>();
        workItemsPairs.add(WorkItemsPair.of(leftWorkItem, rightWorkItem));
        DiffModel diffModel = mock(DiffModel.class);

        List<DiffField> diffFields = new ArrayList<>();
        diffFields.add(DiffField.builder().key("testSteps").build());

        when(diffModel.getDiffFields()).thenReturn(diffFields);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));
        DocumentsMergeParams mergeParams = new DocumentsMergeParams(doc1, doc2, MergeDirection.LEFT_TO_RIGHT, "any", null, "any", workItemsPairs, false);
        DocumentsMergeContext context = new DocumentsMergeContext(polarionService, doc1, doc2, mergeParams.getDirection(), mergeParams.getLinkRole(), diffModel, mergeParams.isAllowedReferencedWorkItemMerge());

        when(polarionService.getWorkItem("project1", "left", null)).thenReturn(leftWorkItem);
        when(polarionService.getWorkItem("project2", "right", null)).thenReturn(rightWorkItem);

        WorkItemsPair pair = WorkItemsPair.of(leftWorkItem, rightWorkItem);

        assertThrows(IllegalStateException.class, () -> mergeService.updateAndMoveItem(pair, context), "Can't merge fields with different types, check that fields with key 'testSteps' have the same type in source and target work item");
    }

    @Test
    void testGetTestStepsData() {
        TestSteps testStepsMock = mock(TestSteps.class);

        ITestStepKeyOpt testStepKeyOptKey = mock(ITestStepKeyOpt.class);
        when(testStepKeyOptKey.getId()).thenReturn("key");
        ITestStepKeyOpt testStepKeyOptDescription = mock(ITestStepKeyOpt.class);
        when(testStepKeyOptDescription.getId()).thenReturn("description");
        ITestStepKeyOpt testStepKeyOptResult = mock(ITestStepKeyOpt.class);
        when(testStepKeyOptResult.getId()).thenReturn("result");
        when(testStepsMock.getKeys()).thenReturn(List.of(testStepKeyOptKey, testStepKeyOptDescription, testStepKeyOptResult));

        TestStep testStepMock = mock(TestStep.class);
        when(testStepMock.getValues()).thenReturn(List.of(Text.plain("key1"), Text.plain("description1"), Text.plain("result1")));
        when(testStepsMock.getSteps()).thenReturn(List.of(testStepMock));

        Map<String, List<?>> testStepsData = mergeService.getTestStepsData(testStepsMock);
        assertEquals(2, testStepsData.size());
        assertTrue(testStepsData.containsKey("keys"));
        assertTrue(testStepsData.containsKey("steps"));

        List<?> keys = testStepsData.get("keys");
        assertEquals(3, keys.size());
        assertEquals("key", keys.get(0));
        assertEquals("description", keys.get(1));
        assertEquals("result", keys.get(2));

        List<?> steps = testStepsData.get("steps");
        assertEquals(1, steps.size());

        List<?> step = (List<?>) ((Map<?,?>) steps.get(0)).get("values");
        assertEquals(3, step.size());
        assertEquals(Text.plain("key1"), step.get(0));
        assertEquals(Text.plain("description1"), step.get(1));
        assertEquals(Text.plain("result1"), step.get(2));
    }

    private IModule mockTargetModule() {
        IModule module = mock(IModule.class, RETURNS_DEEP_STUBS);
        IDataService dataService = mock(IDataService.class);
        when(dataService.createStructureForTypeId(nullable(IPObject.class), nullable(String.class), nullable(Map.class))).thenReturn(mock(IModule.IStructureNode.class));
        when(module.getDataSvc()).thenReturn(dataService);
        return module;
    }

    private void mockCustomFields() {
        CustomField customFieldMock = mock(CustomField.class);
        when(customFieldMock.getType()).thenReturn(mock(IType.class));
        CustomFieldsService customFieldsServiceMock = mock(CustomFieldsService.class);
        when(customFieldsServiceMock.getCustomField(any(), any())).thenReturn(customFieldMock);
        IDataService dataServiceMock = mock(IDataService.class);
        when(dataServiceMock.getCustomFieldsService()).thenReturn(customFieldsServiceMock);
        ITrackerService trackerServiceMock = mock(ITrackerService.class);
        when(trackerServiceMock.getDataService()).thenReturn(dataServiceMock);
        when(polarionService.getTrackerService()).thenReturn(trackerServiceMock);
    }
}

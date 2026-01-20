package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.WorkItemAttachmentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemStatus;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.AuthorizationModel;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.LinkRole;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.NonListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.tracker.IModuleManager;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IHyperlinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.persistence.spi.PObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IType;
import com.polarion.subterra.base.location.ILocation;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolarionServiceTest {

    private static final String PROJECT_ID = "projectId";
    private static final String LINK_ROLE_ID_1 = "roleId1";
    private static final String LINK_ROLE_ID_2 = "roleId2";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITrackerService trackerService;

    @Mock
    private IProjectService projectService;

    @Mock
    private ISecurityService securityService;

    private PolarionService polarionService;

    @Mock
    private InternalWriteTransaction internalWriteTransactionMock;

    private MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic;

    private MockedStatic<DiffModelCachedResource> mockModelCache;

    @BeforeEach
    void init() {
        polarionService = new PolarionService(trackerService, projectService, securityService, null, null);

        mockModelCache = mockStatic(DiffModelCachedResource.class);
        DiffModel cachedModel = mock(DiffModel.class);
        mockModelCache.when(() -> DiffModelCachedResource.get(anyString(), anyString(), nullable(String.class))).thenReturn(cachedModel);

        transactionalExecutorMockedStatic = Mockito.mockStatic(TransactionalExecutor.class);
        internalWriteTransactionMock = mock(InternalWriteTransaction.class);
        transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(invocation -> {
            RunnableInWriteTransaction<?> runnable = invocation.getArgument(0);
            return runnable.run(internalWriteTransactionMock);
        });
        transactionalExecutorMockedStatic.when(TransactionalExecutor::currentTransaction).thenReturn(internalWriteTransactionMock);

    }

    @AfterEach
    void tearDown() {
        mockModelCache.close();
        transactionalExecutorMockedStatic.close();
    }

    @Test
    void testGetAttachmentsWhenMissingProjectId() {
        WorkItemAttachmentIdentifier identifier = WorkItemAttachmentIdentifier.builder().workItemId("wi_id").attachmentId("attachment_id").build();
        assertThrows(IllegalArgumentException.class, () -> polarionService.getAttachment(identifier));
    }

    @Test
    void testGetAttachmentsWhenMissingWorkItemId() {
        WorkItemAttachmentIdentifier identifier = WorkItemAttachmentIdentifier.builder().projectId("project_id").attachmentId("attachment_id").build();
        assertThrows(IllegalArgumentException.class, () -> polarionService.getAttachment(identifier));
    }

    @Test
    void testGetAttachmentsWhenMissingAttachmentId() {
        WorkItemAttachmentIdentifier identifier = WorkItemAttachmentIdentifier.builder().projectId("project_id").workItemId("wi_id").build();
        assertThrows(IllegalArgumentException.class, () -> polarionService.getAttachment(identifier));
    }

    @Test
    void testGetNotExistentWorkItem() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProject);

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.isUnresolvable()).thenReturn(Boolean.TRUE);
        when(trackerProject.getWorkItem(anyString())).thenReturn(workItem);

        assertThrows(ObjectNotFoundException.class, () -> polarionService.getWorkItem("project_id", "wi_id"));
    }

    @Test
    void testGetWorkItem() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProject);

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.isUnresolvable()).thenReturn(Boolean.FALSE);
        when(trackerProject.getWorkItem(anyString())).thenReturn(workItem);

        assertNotNull(polarionService.getWorkItem("project_id", "wi_id"));
    }

    @Test
    void testReplaceLinksToPairedWorkItems() {
        PolarionService service = mock(PolarionService.class);
        when(service.replaceLinksToPairedWorkItems(any(IWorkItem.class), any(IWorkItem.class), anyString(), anyString())).thenCallRealMethod();

        IWorkItem a1 = mock(IWorkItem.class);
        when(service.getWorkItem(eq("projectIdA"), eq("A-1"), isNull())).thenReturn(a1);
        IWorkItem a2 = mock(IWorkItem.class);
        when(service.getWorkItem(eq("projectIdA"), eq("A-2"), isNull())).thenReturn(a2);
        IWorkItem a2r = mock(IWorkItem.class);
        when(service.getWorkItem("projectIdA", "A-2", "42")).thenReturn(a2r);
        IWorkItem a3 = mock(IWorkItem.class);
        when(service.getWorkItem(eq("projectIdA"), eq("A-3"), isNull())).thenReturn(a3);
        IWorkItem c3 = mock(IWorkItem.class);
        when(service.getWorkItem(eq("projectIdC"), eq("C-3"), isNull())).thenReturn(c3);

        when(service.getPairedWorkItems(any(), anyString(), anyString())).thenReturn(List.of());
        IWorkItem b1 = mock(IWorkItem.class);
        when(b1.getId()).thenReturn("B-1");
        when(service.getPairedWorkItems(a1, "projectIdB", "expectedLinkRole")).thenReturn(List.of(b1));
        IWorkItem b2 = mock(IWorkItem.class);
        when(b2.getId()).thenReturn("B-2");
        when(service.getPairedWorkItems(a2, "projectIdB", "expectedLinkRole")).thenReturn(List.of(b2));
        IWorkItem b2r = mock(IWorkItem.class);
        when(b2r.getId()).thenReturn("B-2r");
        when(service.getPairedWorkItems(a2r, "projectIdB", "expectedLinkRole")).thenReturn(List.of(b2r));
        IWorkItem b3 = mock(IWorkItem.class);
        when(b3.getId()).thenReturn("B-3");
        when(service.getPairedWorkItems(c3, "projectIdB", "expectedLinkRole")).thenReturn(List.of(b3));

        IWorkItem from = mock(IWorkItem.class);
        when(from.getProjectId()).thenReturn("projectIdA");
        IWorkItem to = mock(IWorkItem.class);
        when(to.getProjectId()).thenReturn("projectIdB");

        String result = service.replaceLinksToPairedWorkItems(from, to, "expectedLinkRole", """
                item to replace
                <span data-type="workItem" id="fake" data-item-id="A-1" data-option-id="short" class="polarion-rte-link"></span>
                <span>some random span and one more to replace</span>
                <span data-type="workItem" class="polarion-rte-link" id="fake" data-item-id="A-2" data-option-id="short"></span>
                one more but this time with revision which must be cleaned after merge
                <span class="polarion-rte-link" id="fake" data-item-id="A-2" data-revision="42" data-type="workItem" data-option-id="short"></span>
                this has projectId explicitly
                <span class="polarion-rte-link" data-type="workItem" id="fake" data-scope="projectIdC" data-item-id="C-3" data-option-id="short"></span>
                item below has no pair
                <span class="polarion-rte-link" data-type="workItem" id="fake" data-custom-label="Some custom label" data-item-id="A-3" data-option-id="custom"></span>
                item below isn't wi link
                <span class="polarion-rte-link" data-type="nonWorkItem" id="fake" data-item-id="A-1" data-option-id="short"></span>
                item below isn't rte link
                <span class="polarion-non-rte-link" data-type="nonWorkItem" id="fake" data-item-id="A-1" data-option-id="short"></span>
                """);

        assertEquals("""
                item to replace
                <span data-type="workItem" id="fake" data-item-id="B-1" data-option-id="short" class="polarion-rte-link"></span>
                <span>some random span and one more to replace</span>
                <span data-type="workItem" class="polarion-rte-link" id="fake" data-item-id="B-2" data-option-id="short"></span>
                one more but this time with revision which must be cleaned after merge
                <span class="polarion-rte-link" id="fake" data-item-id="B-2r" data-type="workItem" data-option-id="short"></span>
                this has projectId explicitly
                <span class="polarion-rte-link" data-type="workItem" id="fake" data-item-id="B-3" data-option-id="short"></span>
                item below has no pair
                <span class="polarion-rte-link" data-type="workItem" id="fake" data-custom-label="Some custom label" data-item-id="A-3" data-scope="projectIdA" data-option-id="custom"></span>
                item below isn't wi link
                <span class="polarion-rte-link" data-type="nonWorkItem" id="fake" data-item-id="A-1" data-option-id="short"></span>
                item below isn't rte link
                <span class="polarion-non-rte-link" data-type="nonWorkItem" id="fake" data-item-id="A-1" data-option-id="short"></span>
                """, result);
    }

    @Test
    void testGetPairedWorkItems() {

        IProject project = mock(IProject.class);
        lenient().when(projectService.getProject(anyString())).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        lenient().when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProject);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        IModule document1 = mockDocument("one", calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        IModule document2 = mockDocument("two", calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        IModule document3 = mockDocument("three", calendar.getTime());

        IWorkItem doc1Item1 = mockWorkItem(trackerProject, document1, "11", true, false, "done");
        IWorkItem doc1Item2 = mockWorkItem(trackerProject, document1, "12", true, false, "open");
        IWorkItem doc1Item3 = mockWorkItem(trackerProject, document1, "13", false, false, "done");
        IWorkItem doc1Item4 = mockWorkItem(trackerProject, document1, "14", true, false, "open");
        IWorkItem doc1Item5 = mockWorkItem(trackerProject, document1, "15", true, false, "draft");
        IWorkItem doc1Item6 = mockWorkItem(trackerProject, document1, "16", true, false, "open");
        when(document1.getAllWorkItems()).thenReturn(List.of(
                doc1Item1,
                doc1Item2,
                doc1Item3,
                doc1Item4,
                doc1Item5,
                doc1Item6
        ));

        IWorkItem doc2Item1 = mockWorkItem(trackerProject, document2, "21", true, false, "done");
        IWorkItem doc2Item2 = mockWorkItem(trackerProject, document2, "22", true, false, "in_progress");
        IWorkItem doc2Item3 = mockWorkItem(trackerProject, document2, "23", true, true, "done");
        IWorkItem doc2Item4 = mockWorkItem(trackerProject, document2, "24", true, false, "in_progress");
        when(document2.getAllWorkItems()).thenReturn(List.of(
                doc2Item1,
                doc2Item2,
                doc2Item3,
                doc2Item4
        ));

        IWorkItem doc3Item1 = mockWorkItem(trackerProject, document3, "31", true, false, "in_progress");
        IWorkItem doc3Item2 = mockWorkItem(trackerProject, document3, "32", true, false, "in_progress");

        mockLinks(doc1Item1, List.of(mockLink(doc2Item1, LINK_ROLE_ID_1), mockLink(doc2Item1, LINK_ROLE_ID_2), mockLink(doc2Item2, LINK_ROLE_ID_1)), List.of(mockLink(doc2Item2, LINK_ROLE_ID_2)));
        mockLinks(doc1Item2, List.of(mockLink(doc2Item1, LINK_ROLE_ID_1), mockLink(doc2Item2, LINK_ROLE_ID_2)), List.of(mockLink(doc2Item2, LINK_ROLE_ID_1)));
        mockLinks(doc1Item3, List.of(mockLink(doc2Item1, LINK_ROLE_ID_1), mockLink(doc2Item2, LINK_ROLE_ID_1)), List.of());
        mockLinks(doc1Item4, List.of(mockLink(doc2Item4, LINK_ROLE_ID_2)), List.of());
        mockLinks(doc1Item5, List.of(mockLink(doc3Item1, LINK_ROLE_ID_1), mockLink(doc3Item2, LINK_ROLE_ID_2)), List.of());
        mockLinks(doc1Item6, List.of(mockLink(doc1Item6, LINK_ROLE_ID_2)), List.of());

        mockLinks(doc2Item1, List.of(), List.of(mockLink(doc1Item1, LINK_ROLE_ID_1), mockLink(doc1Item2, LINK_ROLE_ID_1), mockLink(doc1Item3, LINK_ROLE_ID_1)));
        mockLinks(doc2Item2, List.of(mockLink(doc1Item2, LINK_ROLE_ID_1)), List.of(mockLink(doc1Item2, LINK_ROLE_ID_2), mockLink(doc1Item3, LINK_ROLE_ID_2)));
        mockLinks(doc2Item3, List.of(), List.of());
        mockLinks(doc2Item4, List.of(), List.of(mockLink(doc1Item4, LINK_ROLE_ID_2)));

        ILinkRoleOpt role = mock(ILinkRoleOpt.class);
        lenient().when(role.getId()).thenReturn(LINK_ROLE_ID_1);
        List<WorkItemsPair> resultList = polarionService.getPairedWorkItems(document1, document2, role, Collections.singletonList("draft"));

        assertEquals(6, resultList.size());
        Set<Pair<String, String>> idsPairSet = resultList.stream().map(
                pair -> Pair.of(pair.getLeftWorkItem() == null ? null : pair.getLeftWorkItem().getId(),
                        pair.getRightWorkItem() == null ? null : pair.getRightWorkItem().getId())).collect(Collectors.toSet());
        assertTrue(idsPairSet.containsAll(List.<Pair<String, String>>of(
                Pair.of("ID-11", "ID-21"),
                Pair.of("ID-11", "ID-22"),
                Pair.of("ID-12", "ID-21"),
                Pair.of("ID-12", "ID-22"),
                Pair.of("ID-16", null),
                Pair.of(null, "ID-23")
        )));
    }

    @Test
    void testGetDeletableFields() {
        List<WorkItemField> standardFieldsDeletable = List.of(WorkItemField.builder().key("field_0").build());

        IContextId contextId = mock(IContextId.class);

        IWorkItem workItem1 = mock(IWorkItem.class);
        when(workItem1.getType()).thenReturn(null);
        List<WorkItemField> result = polarionService.getDeletableFields(workItem1, contextId, standardFieldsDeletable);
        assertEquals(1, result.size());
        assertEquals("field_0", result.get(0).getKey());

        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        ICustomFieldsService customFieldsService = mock(ICustomFieldsService.class);
        when(dataService.getCustomFieldsService()).thenReturn(customFieldsService);

        Collection<ICustomField> customFields = new ArrayList<>();
        ICustomField customField1 = mock(ICustomField.class);
        when(customField1.getId()).thenReturn("field_1");
        customFields.add(customField1);
        ICustomField customField2 = mock(ICustomField.class);
        when(customField2.getId()).thenReturn("field_2");
        customFields.add(customField2);

        ITypeOpt workItemType = mock(ITypeOpt.class);
        when(workItemType.getId()).thenReturn("type");
        when(workItemType.getName()).thenReturn("Type");

        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, null)).thenReturn(List.of());
        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, workItemType.getId())).thenReturn(customFields);

        IWorkItem workItem2 = mock(IWorkItem.class);
        when(workItem2.getType()).thenReturn(workItemType);
        result = polarionService.getDeletableFields(workItem2, contextId, standardFieldsDeletable);
        assertEquals(3, result.size());

        result.sort(Comparator.comparing(WorkItemField::getKey));
        assertEquals("field_0", result.get(0).getKey());
        assertEquals("field_1", result.get(1).getKey());
        assertEquals("field_2", result.get(2).getKey());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testFillStandardFieldDefaultValueForEnums() {
        WorkItemField field = WorkItemField.builder().key(IWorkItem.KEY_SEVERITY).build();

        IWorkItem workItem = mock(IWorkItem.class);
        IContextId contextId = mock(IContextId.class);
        when(workItem.getContextId()).thenReturn(contextId);

        IPrototype workItemPrototype = mock(IPrototype.class);
        when(workItem.getPrototype()).thenReturn(workItemPrototype);
        when(workItemPrototype.getName()).thenReturn("Severity");

        IEnumType enumType = mock(IEnumType.class);
        when(workItemPrototype.getKeyType(field.getKey())).thenReturn(enumType);

        IDataService dataService = mock(IDataService.class);
        when(workItem.getDataSvc()).thenReturn(dataService);

        IEnumeration enumeration = mock(IEnumeration.class);
        when(dataService.getEnumerationForKey(anyString(), anyString(), any(IContextId.class))).thenReturn(enumeration);

        IEnumOption newValue = mock(IEnumOption.class);
        when(enumeration.getDefaultOption(null)).thenReturn(newValue);

        new NonListFieldCleaner().fillStandardFieldDefaultValue(workItem, field);

        verify(workItem, times(1)).setValue(field.getKey(), newValue);
    }

    @Test
    void testFillStandardFieldDefaultValueForNonEnums() {
        WorkItemField field = WorkItemField.builder().key(IWorkItem.KEY_TITLE).build();

        IWorkItem workItem = mock(IWorkItem.class);

        IPrototype workItemPrototype = mock(IPrototype.class);
        when(workItem.getPrototype()).thenReturn(workItemPrototype);

        IType nonEnumType = mock(IType.class);
        when(workItemPrototype.getKeyType(field.getKey())).thenReturn(nonEnumType);

        new NonListFieldCleaner().fillStandardFieldDefaultValue(workItem, field);

        verify(workItem, never()).setValue(eq(field.getKey()), any());
    }

    @Test
    void testIsFieldToCleanUp() {
        List<DiffField> allowedFields = new ArrayList<>();

        ITypeOpt requirement = mock(ITypeOpt.class);
        when(requirement.getId()).thenReturn("requirement");

        ITypeOpt testCase = mock(ITypeOpt.class);
        when(testCase.getId()).thenReturn("testcase");

        // ---------
        // Following fields explicitly skipped during clean up, they shouldn't become empty for consistency
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_PROJECT).build(), null));
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_MODULE).build(), null));
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_TYPE).build(), null));
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_OUTLINE_NUMBER).build(), null));
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_RESOLUTION).build(), null));
        // ---------

        assertTrue(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_TITLE).build(), null));

        allowedFields.add(DiffField.builder().key(IWorkItem.KEY_TITLE).build());
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_TITLE).build(), null));
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_TITLE).build(), testCase));

        allowedFields.clear();
        allowedFields.add(DiffField.builder().key(IWorkItem.KEY_TITLE).wiTypeId(testCase.getId()).build());

        assertTrue(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_TITLE).build(), null));
        assertTrue(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_TITLE).build(), requirement));
        assertFalse(polarionService.isFieldToCleanUp(allowedFields, WorkItemField.builder().key(IWorkItem.KEY_TITLE).build(), testCase));
    }

    @Test
    void testGetWorkItemStatuses() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class, RETURNS_DEEP_STUBS);
        when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProject);

        ITypeOpt issueMock = mock(ITypeOpt.class);
        when(issueMock.getId()).thenReturn("issue");
        when(issueMock.getName()).thenReturn("Issue");

        when(trackerProject.getWorkItemTypeEnum().getAllOptions()).thenReturn(List.of(issueMock));

        IStatusOpt draftGeneralStatusMock = mock(IStatusOpt.class);
        when(draftGeneralStatusMock.getId()).thenReturn("draft");
        when(draftGeneralStatusMock.getName()).thenReturn("Draft");

        IStatusOpt releaseStatusMock = mock(IStatusOpt.class);
        when(releaseStatusMock.getId()).thenReturn("release");
        when(releaseStatusMock.getName()).thenReturn("Release");

        IEnumeration statusEnumerationMock = mock(IEnumeration.class);
        when(trackerProject.getStatusEnum()).thenReturn(statusEnumerationMock);

        lenient().when(statusEnumerationMock.getAvailableOptions(null)).thenReturn(List.of(draftGeneralStatusMock, releaseStatusMock));

        IStatusOpt draftIssueStatusMock = mock(IStatusOpt.class);
        when(draftIssueStatusMock.getId()).thenReturn("draftIssue");
        when(draftIssueStatusMock.getName()).thenReturn("Draft");

        IStatusOpt openStatusMock = mock(IStatusOpt.class);
        when(openStatusMock.getId()).thenReturn("open");
        when(openStatusMock.getName()).thenReturn("Open");

        IStatusOpt inProgressStatusMock = mock(IStatusOpt.class);
        when(inProgressStatusMock.getId()).thenReturn("release");
        when(inProgressStatusMock.getName()).thenReturn("Release");

        lenient().when(statusEnumerationMock.getAvailableOptions("issue")).thenReturn(List.of(draftIssueStatusMock, openStatusMock, inProgressStatusMock));

        Collection<WorkItemStatus> statuses = polarionService.getWorkItemStatuses("projectId");
        assertEquals(4, statuses.size());

        WorkItemStatus draftStatus = statuses.stream().filter(status -> status.getId().equals("draft")).findFirst().orElse(null);
        assertNotNull(draftStatus);
        assertEquals("draft", draftStatus.getId());
        assertEquals("Draft", draftStatus.getName());
        assertNull(draftStatus.getWiTypeId());
        assertNull(draftStatus.getWiTypeName());

        WorkItemStatus draftIssueStatus = statuses.stream().filter(status -> status.getId().equals("draftIssue")).findFirst().orElse(null);
        assertNotNull(draftIssueStatus);
        assertEquals("draftIssue", draftIssueStatus.getId());
        assertEquals("Draft", draftIssueStatus.getName());
        assertEquals("issue", draftIssueStatus.getWiTypeId());
        assertEquals("Issue", draftIssueStatus.getWiTypeName());

        WorkItemStatus releaseStatus = statuses.stream().filter(status -> status.getId().equals("release")).findFirst().orElse(null);
        assertNotNull(releaseStatus);
        assertEquals("release", releaseStatus.getId());
        assertEquals("Release", releaseStatus.getName());
        assertNull(releaseStatus.getWiTypeId());
        assertNull(releaseStatus.getWiTypeName());

        WorkItemStatus openStatus = statuses.stream().filter(status -> status.getId().equals("open")).findFirst().orElse(null);
        assertNotNull(openStatus);
        assertEquals("open", openStatus.getId());
        assertEquals("Open", openStatus.getName());
        assertNull(openStatus.getWiTypeId());
        assertNull(openStatus.getWiTypeName());
    }

    @Test
    void testGetStandardFields() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        IPrototype workItemPrototype = mock(IPrototype.class);
        when(dataService.getPrototype(IWorkItem.PROTO)).thenReturn(workItemPrototype);
        IPObject workItemInstance = mock(IPObject.class);
        when(dataService.createInstance(IWorkItem.PROTO)).thenReturn(workItemInstance);

        when(workItemPrototype.getKeyNames()).thenReturn(Arrays.asList("field_1", "field_2", "field_3"));
        when(workItemPrototype.isKeyDefined("field_1")).thenReturn(true);
        when(workItemPrototype.isKeyRequired("field_1")).thenReturn(true);
        when(workItemPrototype.isKeyReadOnly("field_1")).thenReturn(false);
        when(workItemPrototype.isKeyDefined("field_2")).thenReturn(true);
        when(workItemPrototype.isKeyRequired("field_2")).thenReturn(false);
        when(workItemPrototype.isKeyReadOnly("field_2")).thenReturn(true);
        when(workItemPrototype.isKeyDefined("field_3")).thenReturn(false);

        when(workItemInstance.getFieldLabel("field_1")).thenReturn("Field 1");
        when(workItemInstance.getFieldLabel("field_2")).thenReturn("Field 2");

        List<WorkItemField> standardFields = new ArrayList<>(polarionService.getStandardFields());
        assertEquals(2, standardFields.size());

        standardFields.sort(Comparator.comparing(WorkItemField::getKey));
        WorkItemField field1 = standardFields.get(0);
        assertEquals("field_1", field1.getKey());
        assertEquals("Field 1", field1.getName());
        assertTrue(field1.isRequired());
        assertFalse(field1.isReadOnly());

        WorkItemField field2 = standardFields.get(1);
        assertEquals("field_2", field2.getKey());
        assertEquals("Field 2", field2.getName());
        assertFalse(field2.isRequired());
        assertTrue(field2.isReadOnly());
    }

    @Test
    void testGetCustomFields() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        ICustomFieldsService customFieldsService = mock(ICustomFieldsService.class);
        when(dataService.getCustomFieldsService()).thenReturn(customFieldsService);

        Collection<ICustomField> customFields = new ArrayList<>();
        ICustomField customField1 = mock(ICustomField.class);
        when(customField1.getId()).thenReturn("field_1");
        when(customField1.getName()).thenReturn("Field 1");
        when(customField1.isRequired()).thenReturn(true);
        when(customField1.getDefaultValue()).thenReturn("Default 1");
        customFields.add(customField1);
        ICustomField customField2 = mock(ICustomField.class);
        when(customField2.getId()).thenReturn("field_2");
        when(customField2.getName()).thenReturn("Field 2");
        when(customField2.isRequired()).thenReturn(false);
        when(customField2.getDefaultValue()).thenReturn("Default 2");
        customFields.add(customField2);

        IContextId contextId = mock(IContextId.class);

        ITypeOpt workItemType = mock(ITypeOpt.class);
        when(workItemType.getId()).thenReturn("type");
        when(workItemType.getName()).thenReturn("Type");

        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, null)).thenReturn(List.of());
        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, workItemType.getId())).thenReturn(customFields);

        List<WorkItemField> returnedFields = new ArrayList<>(polarionService.getCustomFields(contextId, workItemType));
        assertEquals(2, returnedFields.size());

        returnedFields.sort(Comparator.comparing(WorkItemField::getKey));
        WorkItemField field1 = returnedFields.get(0);
        assertEquals("field_1", field1.getKey());
        assertEquals("Field 1", field1.getName());
        assertEquals("Default 1", field1.getDefaultValue());
        assertTrue(field1.isRequired());
        assertFalse(field1.isReadOnly());
        assertTrue(field1.isCustomField());
        assertEquals("type", field1.getWiTypeId());
        assertEquals("Type", field1.getWiTypeName());

        WorkItemField field2 = returnedFields.get(1);
        assertEquals("field_2", field2.getKey());
        assertEquals("Field 2", field2.getName());
        assertEquals("Default 2", field2.getDefaultValue());
        assertFalse(field2.isRequired());
        assertFalse(field2.isReadOnly());
        assertTrue(field2.isCustomField());
        assertEquals("type", field2.getWiTypeId());
        assertEquals("Type", field2.getWiTypeName());
    }

    @Test
    void testUserNotAuthorizedForMerge() {
        try {
            AuthorizationSettings settingsMock = mock(AuthorizationSettings.class);
            when(settingsMock.getFeatureName()).thenReturn(AuthorizationSettings.FEATURE_NAME);
            AuthorizationModel authorizationModel = new AuthorizationModel();
            authorizationModel.setGlobalRoles("merger");
            authorizationModel.setProjectRoles("project_merger");
            when(settingsMock.read(ScopeUtils.getScopeFromProject("projectId"), SettingId.fromName(NamedSettings.DEFAULT_NAME), null)).thenReturn(authorizationModel);
            NamedSettingsRegistry.INSTANCE.register(List.of(settingsMock));

            when(securityService.getCurrentUser()).thenReturn("user");
            when(securityService.getRolesForUser("user")).thenReturn(List.of("role1", "role2"));

            IProject projectMock = mock(IProject.class);
            when(projectService.getProject("projectId")).thenReturn(projectMock);

            ITrackerProject trackerProjectMock = mock(ITrackerProject.class);
            when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProjectMock);
            IContextId contextIdMock = mock(IContextId.class);
            when(trackerProjectMock.getContextId()).thenReturn(contextIdMock);
            when(securityService.getRolesForUser("user", contextIdMock)).thenReturn(List.of("role3", "role4"));

            assertFalse(polarionService.userAuthorizedForMerge("projectId"));
        } finally {
            NamedSettingsRegistry.INSTANCE.getAll().clear();
        }
    }

    @Test
    void testUserAuthorizedForMergeByGlobalRole() {
        try {
            AuthorizationSettings settingsMock = mock(AuthorizationSettings.class);
            when(settingsMock.getFeatureName()).thenReturn(AuthorizationSettings.FEATURE_NAME);
            AuthorizationModel authorizationModel = new AuthorizationModel();
            authorizationModel.setGlobalRoles("merger");
            authorizationModel.setProjectRoles("project_merger");
            when(settingsMock.read(ScopeUtils.getScopeFromProject("projectId"), SettingId.fromName(NamedSettings.DEFAULT_NAME), null)).thenReturn(authorizationModel);
            NamedSettingsRegistry.INSTANCE.register(List.of(settingsMock));

            when(securityService.getCurrentUser()).thenReturn("user");
            when(securityService.getRolesForUser("user")).thenReturn(List.of("merger", "role2"));

            assertTrue(polarionService.userAuthorizedForMerge("projectId"));
        } finally {
            NamedSettingsRegistry.INSTANCE.getAll().clear();
        }
    }

    @Test
    void testUserAuthorizedForMergeByProjectRole() {
        try {
            AuthorizationSettings settingsMock = mock(AuthorizationSettings.class);
            when(settingsMock.getFeatureName()).thenReturn(AuthorizationSettings.FEATURE_NAME);
            AuthorizationModel authorizationModel = new AuthorizationModel();
            authorizationModel.setGlobalRoles("merger");
            authorizationModel.setProjectRoles("project_merger");
            when(settingsMock.read(ScopeUtils.getScopeFromProject("projectId"), SettingId.fromName(NamedSettings.DEFAULT_NAME), null)).thenReturn(authorizationModel);
            NamedSettingsRegistry.INSTANCE.register(List.of(settingsMock));

            when(securityService.getCurrentUser()).thenReturn("user");
            when(securityService.getRolesForUser("user")).thenReturn(List.of("role1", "role2"));

            IProject projectMock = mock(IProject.class);
            when(projectService.getProject("projectId")).thenReturn(projectMock);

            ITrackerProject trackerProjectMock = mock(ITrackerProject.class);
            when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProjectMock);
            IContextId contextIdMock = mock(IContextId.class);
            when(trackerProjectMock.getContextId()).thenReturn(contextIdMock);
            when(securityService.getRolesForUser("user", contextIdMock)).thenReturn(List.of("role3", "project_merger"));

            assertTrue(polarionService.userAuthorizedForMerge("projectId"));
        } finally {
            NamedSettingsRegistry.INSTANCE.getAll().clear();
        }
    }

    @Test
    void testGetHyperlinkRoles() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class, RETURNS_DEEP_STUBS);
        when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProject);

        ITypeOpt workItemTypeMock = mock(ITypeOpt.class);
        when(workItemTypeMock.getId()).thenReturn("workItemTypeId");
        when(workItemTypeMock.getName()).thenReturn("Work Item Type");

        when(trackerProject.getWorkItemTypeEnum().getAllOptions()).thenReturn(List.of(workItemTypeMock));

        IHyperlinkRoleOpt iRole1 = mock(IHyperlinkRoleOpt.class);
        when(iRole1.getId()).thenReturn("role1");
        when(iRole1.getName()).thenReturn("Role 1");

        IHyperlinkRoleOpt iRole2 = mock(IHyperlinkRoleOpt.class);
        when(iRole2.getId()).thenReturn("role2");
        when(iRole2.getName()).thenReturn("Role 2");

        when(trackerProject.getHyperlinkRoleEnum().getAvailableOptions("workItemTypeId")).thenReturn(List.of(iRole1, iRole2));

        Collection<LinkRole> linkRoles = polarionService.getHyperlinkRoles("projectId");

        assertEquals(2, linkRoles.size());
        LinkRole role1 = linkRoles.stream().filter(role -> "role1".equals(role.getId())).findFirst().orElse(null);
        assertNotNull(role1);
        assertEquals("Role 1", role1.getName());
        assertEquals("workItemTypeId", role1.getWorkItemTypeId());
        assertEquals("Work Item Type", role1.getWorkItemTypeName());

        LinkRole role2 = linkRoles.stream().filter(role -> "role2".equals(role.getId())).findFirst().orElse(null);
        assertNotNull(role2);
        assertEquals("Role 2", role2.getName());
        assertEquals("workItemTypeId", role2.getWorkItemTypeId());
        assertEquals("Work Item Type", role2.getWorkItemTypeName());
    }

    @Test
    void testGetLinkedWorkItemRoles() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class, RETURNS_DEEP_STUBS);
        when(trackerService.getTrackerProject((IProject) any())).thenReturn(trackerProject);

        ILinkRoleOpt iRole1 = mock(ILinkRoleOpt.class);
        when(iRole1.getId()).thenReturn("role1");
        when(iRole1.getName()).thenReturn("Role 1");

        ILinkRoleOpt iRole2 = mock(ILinkRoleOpt.class);
        when(iRole2.getId()).thenReturn("role2");
        when(iRole2.getName()).thenReturn("Role 2");

        when(trackerProject.getWorkItemLinkRoleEnum().getAllOptions()).thenReturn(List.of(iRole1, iRole2));

        Collection<LinkRole> linkRoles = polarionService.getLinkedWorkItemRoles("projectId");

        assertEquals(2, linkRoles.size());
        LinkRole role1 = linkRoles.stream().filter(role -> "role1".equals(role.getId())).findFirst().orElse(null);
        assertNotNull(role1);
        assertEquals("Role 1", role1.getName());
        assertNull(role1.getWorkItemTypeId());
        assertNull(role1.getWorkItemTypeName());

        LinkRole role2 = linkRoles.stream().filter(role -> "role2".equals(role.getId())).findFirst().orElse(null);
        assertNotNull(role2);
        assertEquals("Role 2", role2.getName());
        assertNull(role2.getWorkItemTypeId());
        assertNull(role2.getWorkItemTypeName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetProjects() {
        IProject project1 = mock(IProject.class, RETURNS_DEEP_STUBS);
        IProject project2 = mock(IProject.class, RETURNS_DEEP_STUBS);
        IProject project3 = mock(IProject.class, RETURNS_DEEP_STUBS);

        when(project1.can().read()).thenReturn(true);
        when(project2.can().read()).thenReturn(false);
        when(project3.can().read()).thenReturn(true);

        IPObjectList<IProject> projectList = mock(IPObjectList.class);
        when(projectList.stream()).thenReturn(java.util.stream.Stream.of(project1, project2, project3));
        when(projectService.searchProjects("", "name")).thenReturn(projectList);

        List<IProject> result = polarionService.getProjects();

        assertEquals(2, result.size());
        assertTrue(result.contains(project1));
        assertTrue(result.contains(project3));
        assertFalse(result.contains(project2));
    }

    @Test
    void testGetSpaces() {
        IFolder folder1 = mock(IFolder.class, RETURNS_DEEP_STUBS);
        IFolder folder2 = mock(IFolder.class, RETURNS_DEEP_STUBS);
        IFolder folder3 = mock(IFolder.class, RETURNS_DEEP_STUBS);

        when(folder1.can().read()).thenReturn(true);
        when(folder2.can().read()).thenReturn(false);
        when(folder3.can().read()).thenReturn(true);

        when(trackerService.getFolderManager().getFolders(PROJECT_ID)).thenReturn(List.of(folder1, folder2, folder3));

        List<IFolder> result = polarionService.getSpaces(PROJECT_ID);

        assertEquals(2, result.size());
        assertTrue(result.contains(folder1));
        assertTrue(result.contains(folder3));
        assertFalse(result.contains(folder2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetDocuments() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        IModuleManager moduleManager = mock(IModuleManager.class);
        when(trackerService.getModuleManager()).thenReturn(moduleManager);

        IModule module1 = mock(IModule.class, RETURNS_DEEP_STUBS);
        IModule module2 = mock(IModule.class, RETURNS_DEEP_STUBS);
        IModule module3 = mock(IModule.class, RETURNS_DEEP_STUBS);

        when(module1.can().read()).thenReturn(true);
        when(module2.can().read()).thenReturn(false);
        when(module3.can().read()).thenReturn(true);

        IPObjectList<IModule> moduleList = mock(IPObjectList.class);
        when(moduleList.stream()).thenReturn(java.util.stream.Stream.of(module1, module2, module3));
        when(moduleManager.getModules(eq(project), any(ILocation.class))).thenReturn(moduleList);

        List<IModule> result = polarionService.getDocuments(PROJECT_ID, "spaceId");

        assertEquals(2, result.size());
        assertTrue(result.contains(module1));
        assertTrue(result.contains(module3));
        assertFalse(result.contains(module2));
    }

    @Test
    void testGetLinkRoles() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class, RETURNS_DEEP_STUBS);
        when(trackerService.getTrackerProject(project)).thenReturn(trackerProject);

        ITypeOpt wiType1 = mock(ITypeOpt.class);
        when(wiType1.getId()).thenReturn("requirement");

        ITypeOpt wiType2 = mock(ITypeOpt.class);
        when(wiType2.getId()).thenReturn("testcase");

        when(trackerProject.getWorkItemTypeEnum().getAllOptions()).thenReturn(List.of(wiType1, wiType2));

        ILinkRoleOpt role1 = mock(ILinkRoleOpt.class);
        when(role1.getId()).thenReturn("parent");

        ILinkRoleOpt role2 = mock(ILinkRoleOpt.class);
        when(role2.getId()).thenReturn("verifies");

        when(trackerProject.getWorkItemLinkRoleEnum().getAvailableOptions("requirement")).thenReturn(List.of(role1));
        when(trackerProject.getWorkItemLinkRoleEnum().getAvailableOptions("testcase")).thenReturn(List.of(role1, role2));

        Collection<ILinkRoleOpt> result = polarionService.getLinkRoles(PROJECT_ID);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> "parent".equals(r.getId())));
        assertTrue(result.stream().anyMatch(r -> "verifies".equals(r.getId())));
    }

    @Test
    void testGetLinkRoleByIdFound() {
        ITrackerProject trackerProject = mock(ITrackerProject.class, RETURNS_DEEP_STUBS);

        ITypeOpt wiType = mock(ITypeOpt.class);
        when(wiType.getId()).thenReturn("requirement");
        when(trackerProject.getWorkItemTypeEnum().getAllOptions()).thenReturn(List.of(wiType));

        ILinkRoleOpt role = mock(ILinkRoleOpt.class);
        when(role.getId()).thenReturn("parent");
        when(trackerProject.getWorkItemLinkRoleEnum().getAvailableOptions("requirement")).thenReturn(List.of(role));

        ILinkRoleOpt result = polarionService.getLinkRoleById("parent", trackerProject);

        assertNotNull(result);
        assertEquals("parent", result.getId());
    }

    @Test
    void testGetLinkRoleByIdNotFound() {
        ITrackerProject trackerProject = mock(ITrackerProject.class, RETURNS_DEEP_STUBS);

        ITypeOpt wiType = mock(ITypeOpt.class);
        when(wiType.getId()).thenReturn("requirement");
        when(trackerProject.getWorkItemTypeEnum().getAllOptions()).thenReturn(List.of(wiType));

        ILinkRoleOpt role = mock(ILinkRoleOpt.class);
        when(role.getId()).thenReturn("parent");
        when(trackerProject.getWorkItemLinkRoleEnum().getAvailableOptions("requirement")).thenReturn(List.of(role));

        ILinkRoleOpt result = polarionService.getLinkRoleById("nonexistent", trackerProject);

        assertNull(result);
    }

    @Test
    void testGetWorkItems() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject(project)).thenReturn(trackerProject);

        IWorkItem workItem1 = mock(IWorkItem.class);
        when(workItem1.isUnresolvable()).thenReturn(false);

        IWorkItem workItem2 = mock(IWorkItem.class);
        when(workItem2.isUnresolvable()).thenReturn(true);

        IWorkItem workItem3 = mock(IWorkItem.class);
        when(workItem3.isUnresolvable()).thenReturn(false);

        when(trackerProject.getWorkItem("WI-1")).thenReturn(workItem1);
        when(trackerProject.getWorkItem("WI-2")).thenReturn(workItem2);
        when(trackerProject.getWorkItem("WI-3")).thenReturn(workItem3);

        List<IWorkItem> result = polarionService.getWorkItems(PROJECT_ID, List.of("WI-1", "WI-2", "WI-3"));

        assertEquals(2, result.size());
        assertTrue(result.contains(workItem1));
        assertTrue(result.contains(workItem3));
        assertFalse(result.contains(workItem2));
    }

    @Test
    void testGetWorkItemsWithEmptyIds() {
        IProject project = mock(IProject.class);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject(project)).thenReturn(trackerProject);

        IWorkItem workItem1 = mock(IWorkItem.class);
        when(workItem1.isUnresolvable()).thenReturn(false);
        when(trackerProject.getWorkItem("WI-1")).thenReturn(workItem1);

        List<String> idsWithEmpty = new ArrayList<>();
        idsWithEmpty.add("WI-1");
        idsWithEmpty.add("");
        idsWithEmpty.add("  ");
        idsWithEmpty.add(null);

        List<IWorkItem> result = polarionService.getWorkItems(PROJECT_ID, idsWithEmpty);

        assertEquals(1, result.size());
        assertTrue(result.contains(workItem1));
    }

    @Test
    void testGetPairedWorkItemsWithLinkRole() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);

        ILinkedWorkItemStruct linkStruct1 = mock(ILinkedWorkItemStruct.class);
        ILinkRoleOpt linkRole1 = mock(ILinkRoleOpt.class);
        when(linkRole1.getId()).thenReturn("pairedWith");
        when(linkStruct1.getLinkRole()).thenReturn(linkRole1);

        IWorkItem linkedWorkItem1 = mock(IWorkItem.class);
        when(linkedWorkItem1.isUnresolvable()).thenReturn(false);
        when(linkedWorkItem1.getProjectId()).thenReturn("targetProject");
        when(linkStruct1.getLinkedItem()).thenReturn(linkedWorkItem1);

        ILinkedWorkItemStruct linkStruct2 = mock(ILinkedWorkItemStruct.class);
        ILinkRoleOpt linkRole2 = mock(ILinkRoleOpt.class);
        lenient().when(linkRole2.getId()).thenReturn("otherRole");
        when(linkStruct2.getLinkRole()).thenReturn(linkRole2);

        IWorkItem linkedWorkItem2 = mock(IWorkItem.class);
        lenient().when(linkedWorkItem2.isUnresolvable()).thenReturn(false);
        lenient().when(linkedWorkItem2.getProjectId()).thenReturn("targetProject");
        lenient().when(linkStruct2.getLinkedItem()).thenReturn(linkedWorkItem2);

        when(sourceWorkItem.getLinkedWorkItemsStructsDirect()).thenReturn(List.of(linkStruct1));
        when(sourceWorkItem.getLinkedWorkItemsStructsBack()).thenReturn(List.of(linkStruct2));

        List<IWorkItem> result = polarionService.getPairedWorkItems(sourceWorkItem, "targetProject", "pairedWith");

        assertEquals(1, result.size());
        assertEquals(linkedWorkItem1, result.get(0));
    }

    @Test
    void testGetPairedWorkItemsWithDifferentProject() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);

        ILinkedWorkItemStruct linkStruct = mock(ILinkedWorkItemStruct.class);
        ILinkRoleOpt linkRole = mock(ILinkRoleOpt.class);
        when(linkRole.getId()).thenReturn("pairedWith");
        when(linkStruct.getLinkRole()).thenReturn(linkRole);

        IWorkItem linkedWorkItem = mock(IWorkItem.class);
        when(linkedWorkItem.isUnresolvable()).thenReturn(false);
        when(linkedWorkItem.getProjectId()).thenReturn("otherProject");
        when(linkStruct.getLinkedItem()).thenReturn(linkedWorkItem);

        when(sourceWorkItem.getLinkedWorkItemsStructsDirect()).thenReturn(List.of(linkStruct));
        when(sourceWorkItem.getLinkedWorkItemsStructsBack()).thenReturn(List.of());

        List<IWorkItem> result = polarionService.getPairedWorkItems(sourceWorkItem, "targetProject", "pairedWith");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPairedWorkItemsWithNullLinkRole() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);

        ILinkedWorkItemStruct linkStruct = mock(ILinkedWorkItemStruct.class);
        when(linkStruct.getLinkRole()).thenReturn(null);

        when(sourceWorkItem.getLinkedWorkItemsStructsDirect()).thenReturn(List.of(linkStruct));
        when(sourceWorkItem.getLinkedWorkItemsStructsBack()).thenReturn(List.of());

        List<IWorkItem> result = polarionService.getPairedWorkItems(sourceWorkItem, "targetProject", "pairedWith");

        assertTrue(result.isEmpty());
    }

    private IModule mockDocument(String name, Date created) {
        IModule document = mock(IModule.class);
        lenient().when(document.getProjectId()).thenReturn(PROJECT_ID);
        lenient().when(document.getModuleNameWithSpace()).thenReturn(name);
        lenient().when(document.getCreated()).thenReturn(created);
        return document;
    }

    private IWorkItem mockWorkItem(ITrackerProject trackerProject, IModule module, String index, boolean hasOutlineNumber, boolean referenced, String status) {
        IStatusOpt statusOpt = mock(IStatusOpt.class);
        lenient().when(statusOpt.getId()).thenReturn(status);

        IWorkItem workItem = mock(IWorkItem.class);
        lenient().when(workItem.getProjectId()).thenReturn(PROJECT_ID);
        lenient().when(workItem.getModule()).thenReturn(module);
        lenient().when(workItem.getId()).thenReturn("ID-" + index);
        lenient().when(workItem.getTitle()).thenReturn("Title " + index);
        String outlineNumber = hasOutlineNumber ? "TEST-" + index : null;
        lenient().when(workItem.getOutlineNumber()).thenReturn(outlineNumber);
        lenient().when(module.getOutlineNumberOfWorkitem(workItem)).thenReturn(outlineNumber);

        lenient().when(workItem.getStatus()).thenReturn(statusOpt);
        lenient().when(workItem.getExternalLinkingModules()).thenReturn(
                new PObjectList(mock(IDataService.class), referenced ? Collections.singletonList(module) : Collections.emptyList()));
        lenient().when(trackerProject.getWorkItem("ID-" + index)).thenReturn(workItem);
        return workItem;
    }

    private void mockLinks(IWorkItem workItem, List<ILinkedWorkItemStruct> direct, List<ILinkedWorkItemStruct> back) {
        lenient().when(workItem.getLinkedWorkItemsStructsDirect()).thenReturn(direct);
        lenient().when(workItem.getLinkedWorkItemsStructsBack()).thenReturn(back);
    }

    private ILinkedWorkItemStruct mockLink(IWorkItem linkedItem, String linkedRoleId) {
        ILinkedWorkItemStruct link = mock(ILinkedWorkItemStruct.class);
        lenient().when(link.getLinkedItem()).thenReturn(linkedItem);

        ILinkRoleOpt role = mock(ILinkRoleOpt.class);
        lenient().when(role.getId()).thenReturn(linkedRoleId);
        lenient().when(link.getLinkRole()).thenReturn(role);
        return link;
    }
}

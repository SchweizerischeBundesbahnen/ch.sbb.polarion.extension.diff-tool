package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.BaseDocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentDuplicateParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.HandleReferencesType;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.ListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.NonListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.security.ISecurityService;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.identification.IObjectId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import lombok.SneakyThrows;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class DocumentCopyServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITrackerService trackerService;

    private PolarionService polarionService;

    private DocumentCopyService copyService;

    @Mock
    private IProjectService projectService;

    @Mock
    private ISecurityService securityService;

    @Mock
    private InternalWriteTransaction internalWriteTransactionMock;

    private MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic;

    private MockedStatic<DiffModelCachedResource> mockModelCache;

    @BeforeEach
    void init() {
        polarionService = spy(new PolarionService(trackerService, projectService, securityService, null, null));
        copyService = spy(new DocumentCopyService(polarionService, mock(MergeService.class)));

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
    @SneakyThrows
    void testCreateDocumentDuplicate() {
        when(trackerService.getFolderManager().existFolder(anyString(), anyString())).thenReturn(true);

        IModule module = mock(IModule.class, RETURNS_DEEP_STUBS);
        List<IWorkItem> externalWorkItems = List.of(mock(IWorkItem.class));
        when(module.getExternalWorkItems()).thenReturn(externalWorkItems);
        when(trackerService.getModuleManager().duplicate(any(), any(), any(), any(), nullable(ILinkRoleOpt.class), isNull(), isNull(), isNull(), isNull())).thenReturn(module);

        BaseDocumentIdentifier targetDocumentIdentifier = mock(BaseDocumentIdentifier.class);
        when(targetDocumentIdentifier.getProjectId()).thenReturn("targetProjectId");
        when(targetDocumentIdentifier.getName()).thenReturn("targetModuleName");
        when(targetDocumentIdentifier.getSpaceId()).thenReturn("targetSpaceId");

        IProject sourceProject = mock(IProject.class);
        IProject targetProject = mock(IProject.class);
        when(projectService.getProject("srcProj")).thenReturn(sourceProject);
        when(projectService.getProject("targetProjectId")).thenReturn(targetProject);

        DocumentDuplicateParams duplicateParams = new DocumentDuplicateParams(targetDocumentIdentifier, "targetTitle", "linkRoleId", "configName", HandleReferencesType.DEFAULT);

        IModule sourceModule = mock(IModule.class);
        when(trackerService.getModuleManager().getModule(eq(sourceProject), any())).thenReturn(sourceModule);
        IDataService sourceModuleDataService = mock(IDataService.class);
        when(sourceModuleDataService.getVersionedInstance(any(IObjectId.class), anyString())).thenReturn(sourceModule);
        when(sourceModule.getDataSvc()).thenReturn(sourceModuleDataService);
        when(sourceModule.getObjectId()).thenReturn(mock(IObjectId.class));

        IModule existingModule = mock(IModule.class);
        when(existingModule.isUnresolvable()).thenReturn(false);
        when(trackerService.getModuleManager().getModule(eq(targetProject), any())).thenReturn(existingModule);

        when(trackerService.getDataService().getPrototype(IWorkItem.PROTO).getKeyNames()).thenReturn(List.of());
//        when(trackerService.getFolderManager()).thenReturn(List.of());

        // attempt to copy on top of existing module
        Exception exception = assertThrows(IllegalStateException.class, () ->
                copyService.createDocumentDuplicate("srcProj", "srcSpace", "srcDocName", "123", duplicateParams));
        assertEquals("Document 'targetModuleName' already exists in project 'targetProjectId' and space 'targetSpaceId'", exception.getMessage());

        when(existingModule.isUnresolvable()).thenReturn(true);

        duplicateParams.setTargetDocumentTitle("");
        // empty doc title
        exception = assertThrows(IllegalArgumentException.class, () ->
                copyService.createDocumentDuplicate("srcProj", "srcSpace", "srcDocName", "123", duplicateParams));
        assertEquals("Target 'documentName' should be provided", exception.getMessage());
        duplicateParams.setTargetDocumentTitle("title");

        // attempt to use unknown link role
        exception = assertThrows(IllegalArgumentException.class, () ->
                copyService.createDocumentDuplicate("srcProj", "srcSpace", "srcDocName", "123", duplicateParams));
        assertEquals("No link role could be found by ID 'linkRoleId'", exception.getMessage());

        // now let's omit the link role
        duplicateParams.setLinkRoleId(null);

        when(polarionService.getTrackerService().getFolderManager().existFolder(anyString(), anyString())).thenReturn(false);
        assertDoesNotThrow(() ->
                copyService.createDocumentDuplicate("srcProj", "srcSpace", "srcDocName", "123", duplicateParams));
        verify(copyService, times(0)).fixLinksInRichTextFields(any(), any(), nullable(String.class), any());
        verify(trackerService.getFolderManager(), times(1)).createFolder(nullable(String.class), nullable(String.class), nullable(String.class));

        // provide link role + make it available
        duplicateParams.setLinkRoleId("linkRoleId");
        ILinkRoleOpt linkRole = mock(ILinkRoleOpt.class);
        when(linkRole.getId()).thenReturn("linkRoleId");
        doReturn(linkRole).when(polarionService).getLinkRoleById(anyString(), any());

        assertDoesNotThrow(() ->
                copyService.createDocumentDuplicate("srcProj", "srcSpace", "srcDocName", "123", duplicateParams));
        verify(copyService, times(1)).fixLinksInRichTextFields(any(), any(), nullable(String.class), any());
    }

    @Test
    void testFixLinksInRichTextFields() {
        IModule targetModule = mock(IModule.class, RETURNS_DEEP_STUBS);
        DiffField diffField = mock(DiffField.class);
        when(diffField.getKey()).thenReturn("richTextField");

        IWorkItem workItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
        when(targetModule.getExternalWorkItems()).thenReturn(List.of());
        when(targetModule.getAllWorkItems()).thenReturn(List.of(workItem));

        when(workItem.getCustomField("richTextField")).thenReturn(Text.html("richTextField"));

        IWorkItem pairedWorkItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
        doReturn(List.of(pairedWorkItem)).when(polarionService).getPairedWorkItems(any(), anyString(), anyString());

        copyService.fixLinksInRichTextFields(targetModule, "sourceProject", "linkRole", List.of(diffField));

        verify(polarionService).setFieldValue(workItem, "richTextField", Text.html("richTextField"));
    }

    @Test
    void testCleanUpNonListFields() {

        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        IPrototype workItemPrototype = mock(IPrototype.class);
        when(workItemPrototype.getKeyType(anyString())).thenReturn(mock(IType.class));
        when(dataService.getPrototype(IWorkItem.PROTO)).thenReturn(workItemPrototype);
        IPObject workItemInstance = mock(IPObject.class);
        when(dataService.createInstance(IWorkItem.PROTO)).thenReturn(workItemInstance);

        when(workItemPrototype.getKeyNames()).thenReturn(Arrays.asList("field_1", "field_2"));
        when(workItemPrototype.isKeyDefined("field_1")).thenReturn(true);
        when(workItemPrototype.isKeyRequired("field_1")).thenReturn(true);
        when(workItemPrototype.isKeyReadOnly("field_1")).thenReturn(false);
        when(workItemPrototype.isKeyDefined("field_2")).thenReturn(true);
        when(workItemPrototype.isKeyRequired("field_2")).thenReturn(false);
        when(workItemPrototype.isKeyReadOnly("field_2")).thenReturn(true);

        when(workItemInstance.getFieldLabel("field_1")).thenReturn("Field 1");
        when(workItemInstance.getFieldLabel("field_2")).thenReturn("Field 2");

        ICustomFieldsService customFieldsService = mock(ICustomFieldsService.class);
        when(dataService.getCustomFieldsService()).thenReturn(customFieldsService);

        Collection<ICustomField> customFields = new ArrayList<>();
        ICustomField customField1 = mock(ICustomField.class);
        when(customField1.getId()).thenReturn("field_3");
        when(customField1.getName()).thenReturn("Field 3");
        when(customField1.isRequired()).thenReturn(true);
        when(customField1.getDefaultValue()).thenReturn("Default 3");
        customFields.add(customField1);
        ICustomField customField2 = mock(ICustomField.class);
        when(customField2.getId()).thenReturn("field_4");
        when(customField2.getName()).thenReturn("Field 4");
        when(customField2.isRequired()).thenReturn(false);
        when(customField2.getDefaultValue()).thenReturn("Default 4");
        customFields.add(customField2);

        IContextId contextId = mock(IContextId.class);

        ITypeOpt workItemType = mock(ITypeOpt.class);
        when(workItemType.getId()).thenReturn("type");
        when(workItemType.getName()).thenReturn("Type");

        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, null)).thenReturn(customFields);
        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, workItemType.getId())).thenReturn(List.of());

        IModule module = mock(IModule.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getType()).thenReturn(workItemType);
        when(workItem.getPrototype()).thenReturn(workItemPrototype);
        when(module.getAllWorkItems()).thenReturn(List.of(workItem));
        copyService.cleanUpFields(module, contextId, Collections.emptyList(), new NonListFieldCleaner());

        verify(workItem, never()).setValue(eq("field_1"), any());
        verify(workItem, never()).setValue(eq("field_2"), any());
        verify(workItem, times(1)).setValue(eq("field_3"), eq("Default 3"));
        verify(workItem, times(1)).setValue(eq("field_4"), eq(null));
    }

    @Test
    void testCleanUpListFields() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        ICustomFieldsService customFieldsService = mock(ICustomFieldsService.class);
        when(dataService.getCustomFieldsService()).thenReturn(customFieldsService);

        IPrototype workItemPrototype = mock(IPrototype.class);
        when(dataService.getPrototype(IWorkItem.PROTO)).thenReturn(workItemPrototype);
        IPObject workItemInstance = mock(IPObject.class);
        when(dataService.createInstance(IWorkItem.PROTO)).thenReturn(workItemInstance);

        when(workItemPrototype.getKeyNames()).thenReturn(List.of(IWorkItem.KEY_APPROVALS));
        when(workItemPrototype.isKeyDefined(IWorkItem.KEY_APPROVALS)).thenReturn(true);

        IContextId contextId = mock(IContextId.class);

        ITypeOpt workItemType = mock(ITypeOpt.class);
        when(workItemType.getId()).thenReturn("type");

        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, null)).thenReturn(Collections.emptyList());
        when(customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, workItemType.getId())).thenReturn(Collections.emptyList());

        IModule module = mock(IModule.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getType()).thenReturn(workItemType);
        when(workItem.getFieldType(anyString())).thenReturn(mock(IListType.class));
        when(module.getAllWorkItems()).thenReturn(List.of(workItem));

        IUser user = mock(IUser.class);
        IApprovalStruct approvalStruct = mock(IApprovalStruct.class);
        when(approvalStruct.getUser()).thenReturn(user);
        when(workItem.getApprovals()).thenReturn(List.of(approvalStruct));

        copyService.cleanUpFields(module, contextId, Collections.emptyList(), new ListFieldCleaner());

        verify(workItem, times(1)).removeApprovee(user);
    }

}

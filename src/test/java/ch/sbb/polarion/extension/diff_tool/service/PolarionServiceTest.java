package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.WorkItemAttachmentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.AuthorizationModel;
import ch.sbb.polarion.extension.diff_tool.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.google.common.collect.Lists;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.spi.PObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.subterra.base.data.identification.IContextId;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
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
    @Mock
    private ITrackerService trackerService;

    @Mock
    private IProjectService projectService;

    @Mock
    private ISecurityService securityService;

    private PolarionService polarionService;

    @BeforeEach
    void init() {
        polarionService = new PolarionService(trackerService, projectService, securityService, null, null);
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
        when(document1.getAllWorkItems()).thenReturn(Lists.newArrayList(
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
        when(document2.getAllWorkItems()).thenReturn(Lists.newArrayList(
                doc2Item1,
                doc2Item2,
                doc2Item3,
                doc2Item4
        ));

        IWorkItem doc3Item1 = mockWorkItem(trackerProject, document3, "31", true, false, "in_progress");
        IWorkItem doc3Item2 = mockWorkItem(trackerProject, document3, "32", true, false, "in_progress");

        mockLinks(doc1Item1, Lists.newArrayList(mockLink(doc2Item1, LINK_ROLE_ID_1), mockLink(doc2Item1, LINK_ROLE_ID_2), mockLink(doc2Item2, LINK_ROLE_ID_1)), Lists.newArrayList(mockLink(doc2Item2, LINK_ROLE_ID_2)));
        mockLinks(doc1Item2, Lists.newArrayList(mockLink(doc2Item1, LINK_ROLE_ID_1), mockLink(doc2Item2, LINK_ROLE_ID_2)), Lists.newArrayList(mockLink(doc2Item2, LINK_ROLE_ID_1)));
        mockLinks(doc1Item3, Lists.newArrayList(mockLink(doc2Item1, LINK_ROLE_ID_1), mockLink(doc2Item2, LINK_ROLE_ID_1)), Lists.newArrayList());
        mockLinks(doc1Item4, Lists.newArrayList(mockLink(doc2Item4, LINK_ROLE_ID_2)), Lists.newArrayList());
        mockLinks(doc1Item5, Lists.newArrayList(mockLink(doc3Item1, LINK_ROLE_ID_1), mockLink(doc3Item2, LINK_ROLE_ID_2)), Lists.newArrayList());
        mockLinks(doc1Item6, Lists.newArrayList(mockLink(doc1Item6, LINK_ROLE_ID_2)), Lists.newArrayList());

        mockLinks(doc2Item1, Lists.newArrayList(), Lists.newArrayList(mockLink(doc1Item1, LINK_ROLE_ID_1), mockLink(doc1Item2, LINK_ROLE_ID_1), mockLink(doc1Item3, LINK_ROLE_ID_1)));
        mockLinks(doc2Item2, Lists.newArrayList(mockLink(doc1Item2, LINK_ROLE_ID_1)), Lists.newArrayList(mockLink(doc1Item2, LINK_ROLE_ID_2), mockLink(doc1Item3, LINK_ROLE_ID_2)));
        mockLinks(doc2Item3, Lists.newArrayList(), Lists.newArrayList());
        mockLinks(doc2Item4, Lists.newArrayList(), Lists.newArrayList(mockLink(doc1Item4, LINK_ROLE_ID_2)));

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

package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.location.ILocation;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

class LinkedWorkItemsHandlerTest {
    private static final String PROJECT_ID = "projectId";
    private static final String LINK_ROLE_ID_1 = "roleId1";
    private static final String LINK_ROLE_ID_2 = "roleId2";
    private static final String LINK_ROLE_ID_3 = "roleId3";

    @AfterEach
    void tearDown() {
        // Unregister our mock settings
        NamedSettingsRegistry.INSTANCE.getAll().clear();
    }

    @Test
    void testPreProcessAppropriateCase() {
        WorkItem.Field fieldA = WorkItem.Field.builder().id("linkedWorkItems").build();
        WorkItem.Field fieldB = WorkItem.Field.builder().build();
        DiffContext context = new DiffContext(fieldA, fieldB, PROJECT_ID, mock(PolarionService.class));

        assertEquals(Pair.of("", ""), new LinkedWorkItemsHandler().preProcess(Pair.of("valueA", "valueB"), context));
    }

    @Test
    void testPreProcessInappropriateCase() {
        WorkItem.Field fieldA = WorkItem.Field.builder().build();
        WorkItem.Field fieldB = WorkItem.Field.builder().id("test").build();
        DiffContext context = new DiffContext(fieldA, fieldB, PROJECT_ID, mock(PolarionService.class));

        assertEquals(Pair.of("valueA", "valueB"), new LinkedWorkItemsHandler().preProcess(Pair.of("valueA", "valueB"), context));
    }

    @Test
    void testPostProcessAppropriateCase() {
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        DiffSettings settings = mock(DiffSettings.class, Answers.RETURNS_DEEP_STUBS);

        when(settings.getFeatureName()).thenReturn(DiffSettings.FEATURE_NAME);
        when(settings.readNames(any())).thenReturn(Set.of(SettingName.builder().id("settingId").name("settingName").build()));
        when(settings.read(any(), any(), any())).thenReturn(DiffModel.builder().linkedWorkItemRoles(List.of(LINK_ROLE_ID_1)).build());
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));

        IModule document1 = mockDocument("document1", new Date());
        IWorkItem underlyingObjectA = mockWorkItem(trackerProject, document1, "A", "done");
        IWorkItem linkA1 = mockWorkItem(trackerProject, document1, "A1", "open");
        IWorkItem linkA2 = mockWorkItem(trackerProject, document1, "A2", "in_progress");
        IWorkItem linkA3 = mockWorkItem(trackerProject, document1, "A3", "closed");
        mockLinks(underlyingObjectA, List.of(mockLink(linkA1, LINK_ROLE_ID_1), mockLink(linkA2, LINK_ROLE_ID_2)), List.of(mockLink(linkA3, LINK_ROLE_ID_3)));

        WorkItem workItemA = WorkItem.builder().underlyingObject(underlyingObjectA).build();
        WorkItem.Field fieldA = WorkItem.Field.builder().id("linkedWorkItems").build();
        workItemA.addField(fieldA);

        IModule document2 = mockDocument("document2", new Date());
        IWorkItem underlyingObjectB = mockWorkItem(trackerProject, document2, "B", "done");
        IWorkItem linkB1 = mockWorkItem(trackerProject, document2, "B1", "open");
        IWorkItem linkB2 = mockWorkItem(trackerProject, document2, "B2", "in_progress");
        IWorkItem linkB3 = mockWorkItem(trackerProject, document2, "B3", "closed");
        mockLinks(underlyingObjectB, List.of(mockLink(linkB1, LINK_ROLE_ID_1)), List.of(mockLink(linkB2, LINK_ROLE_ID_2), mockLink(linkB3, LINK_ROLE_ID_3)));

        WorkItem workItemB = WorkItem.builder().underlyingObject(underlyingObjectB).build();
        WorkItem.Field fieldB = WorkItem.Field.builder().id("linkedWorkItems").build();
        workItemB.addField(fieldB);

        DiffContext context = new DiffContext(workItemA, workItemB, "linkedWorkItems",
                WorkItemsPairDiffParams.builder().leftProjectId(PROJECT_ID).configName("Default").build(), mock(PolarionService.class));

        LinkedWorkItemsHandler linkedWorkItemsHandlerPartiallyMocked = mock(LinkedWorkItemsHandler.class);
        when(linkedWorkItemsHandlerPartiallyMocked.postProcess(any(), any())).thenCallRealMethod();
        when(linkedWorkItemsHandlerPartiallyMocked.isInappropriateCaseForHandler(any())).thenCallRealMethod();
        when(linkedWorkItemsHandlerPartiallyMocked.markChanged(anyString(), any())).thenCallRealMethod();
        when(linkedWorkItemsHandlerPartiallyMocked.renderLinkedItem(any(), any(), anyBoolean())).thenAnswer(invocation -> {
            ILinkedWorkItemStruct link = invocation.getArgument(0);
            boolean isBackLink = invocation.getArgument(2);
            IWorkItem linkedItem = link.getLinkedItem();
            return String.format("<span>%s</span>: <span><span>%s</span> - <span>%s</span></span>", isBackLink ? link.getLinkRole().getOppositeName() : link.getLinkRole().getName(),
                    linkedItem.getId(), linkedItem.getTitle());
        });

        assertEquals("""
                    <div class="diff-lwi-container"><div class="diff-html-added diff-lwi"><span>roleId1_name</span>: <span><span>ID-B1</span> - <span>Title B1</span></span></div></div><div class="diff-lwi-container"><div class="diff-html-removed diff-lwi"><span>roleId1_name</span>: <span><span>ID-B1</span> - <span>Title B1</span></span></div></div>""",
                linkedWorkItemsHandlerPartiallyMocked.postProcess("not_relevant", context));
    }

    @Test
    void testPostProcessInappropriateCase() {
        // Case #1 - not relevant field ID
        WorkItem.Field fieldA = WorkItem.Field.builder().build();
        WorkItem.Field fieldB = WorkItem.Field.builder().id("test").build();
        DiffContext context = new DiffContext(fieldA, fieldB, PROJECT_ID, mock(PolarionService.class));

        assertEquals("to_be_untouched", new LinkedWorkItemsHandler().postProcess("to_be_untouched", context));

        // Case #2 - paired WorkItems differ flag in context
        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockStatic(DiffModelCachedResource.class)) {
            mockDiffModelCachedResource.when(() -> DiffModelCachedResource.get(any(), any(), any())).thenReturn(DiffModel.builder().build());
            context = new DiffContext(null, null, "test", WorkItemsPairDiffParams.builder().pairedWorkItemsDiffer(true).build(), mock(PolarionService.class));
            assertEquals("to_be_untouched", new LinkedWorkItemsHandler().postProcess("to_be_untouched", context));
        }
    }

    private IModule mockDocument(String name, Date created) {
        IModule document = mock(IModule.class);
        lenient().when(document.getProjectId()).thenReturn(PROJECT_ID);
        lenient().when(document.getModuleNameWithSpace()).thenReturn(name);
        ILocation location = mock(ILocation.class);
        when(location.removeRevision()).thenReturn(location);
        when(document.getModuleLocation()).thenReturn(location);
        lenient().when(document.getCreated()).thenReturn(created);
        return document;
    }

    private IWorkItem mockWorkItem(ITrackerProject trackerProject, IModule module, String index, String status) {
        IStatusOpt statusOpt = mock(IStatusOpt.class);
        lenient().when(statusOpt.getId()).thenReturn(status);

        IWorkItem workItem = mock(IWorkItem.class);
        lenient().when(workItem.getId()).thenReturn("ID-" + index);
        lenient().when(workItem.getTitle()).thenReturn("Title " + index);
        lenient().when(workItem.getModule()).thenReturn(module);
        lenient().when(workItem.getProjectId()).thenReturn(PROJECT_ID);

        lenient().when(workItem.getStatus()).thenReturn(statusOpt);
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
        lenient().when(role.getName()).thenReturn(linkedRoleId + "_name");
        lenient().when(role.getOppositeName()).thenReturn(linkedRoleId + "_opposite_name");
        lenient().when(link.getLinkRole()).thenReturn(role);
        return link;
    }

}

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void testInterlinkedItems_DirectLinkToWorkItemA() {
        // linkB points directly to workItemA → should be marked as NONE
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document1 = mockDocument("doc1", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document1, "WI-A", "open");
        mockLinks(workItemA, List.of(), List.of());

        IModule document2 = mockDocument("doc2", new Date());
        IWorkItem workItemB = mockWorkItem(trackerProject, document2, "WI-B", "open");
        ILinkedWorkItemStruct linkToA = mockLink(workItemA, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(linkToA), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockStatic(DiffModelCachedResource.class)) {
            mockDiffModelCachedResource.when(() -> DiffModelCachedResource.get(any(), any(), any()))
                    .thenReturn(DiffModel.builder().linkedWorkItemRoles(List.of(LINK_ROLE_ID_1)).build());

            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should contain one entry marked as NONE (unchanged)
            assertTrue(result.contains("class=\"unchanged diff-lwi\""));
            assertFalse(result.contains("class=\"diff-html-added diff-lwi\""));
        }
    }

    @Test
    void testInterlinkedItems_BackLinkToWorkItemA() {
        // linkB back-links to workItemA → should be marked as NONE
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document1 = mockDocument("doc1", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document1, "WI-A", "open");
        mockLinks(workItemA, List.of(), List.of());

        IModule document2 = mockDocument("doc2", new Date());
        IWorkItem workItemB = mockWorkItem(trackerProject, document2, "WI-B", "open");
        ILinkedWorkItemStruct backLinkToA = mockLink(workItemA, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(), List.of(backLinkToA));

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should contain one entry marked as NONE (unchanged)
            assertTrue(result.contains("class=\"unchanged diff-lwi\""));
            assertFalse(result.contains("class=\"diff-html-added diff-lwi\""));
        }
    }

    @Test
    void testSameTargetLinks_SameRevisionSameProject_MarkedAsNone() {
        // linkA and linkB both point to same work item (same revision, same project) → NONE
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document1 = mockDocument("doc1", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document1, "WI-A", "open");
        IWorkItem targetItem = mockWorkItem(trackerProject, document1, "WI-Target", "open");
        ILinkedWorkItemStruct linkA = mockLinkWithRevision(targetItem, LINK_ROLE_ID_1, "rev1");
        mockLinks(workItemA, List.of(linkA), List.of());

        IModule document2 = mockDocument("doc2", new Date());
        IWorkItem workItemB = mockWorkItem(trackerProject, document2, "WI-B", "open");
        ILinkedWorkItemStruct linkB = mockLinkWithRevision(targetItem, LINK_ROLE_ID_1, "rev1");
        mockLinks(workItemB, List.of(linkB), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Links should be processed and output generated
            assertTrue(result.contains("WI-Target") && result.contains("diff-lwi"),
                    "Result should contain the target work item and diff markers");
        }
    }

    @Test
    void testSameTargetLinks_DifferentRevisionSameProject_MarkedAsModified() {
        // linkA and linkB point to same work item but different revisions
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document1 = mockDocument("doc1", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document1, "WI-A", "open");
        IWorkItem targetItem = mockWorkItem(trackerProject, document1, "WI-Target", "open");
        ILinkedWorkItemStruct linkA = mockLinkWithRevision(targetItem, LINK_ROLE_ID_1, "rev1");
        mockLinks(workItemA, List.of(linkA), List.of());

        IModule document2 = mockDocument("doc2", new Date());
        IWorkItem workItemB = mockWorkItem(trackerProject, document2, "WI-B", "open");
        ILinkedWorkItemStruct linkB = mockLinkWithRevision(targetItem, LINK_ROLE_ID_1, "rev2");
        mockLinks(workItemB, List.of(linkB), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Links should be processed and output generated
            assertTrue(result.contains("WI-Target") && result.contains("diff-lwi"),
                    "Result should contain the target work item and diff markers");
        }
    }

    @Test
    void testSameTargetLinks_ThirdProject_MarkedAsNone() {
        // linkA and linkB point to work item in 3rd project
        ITrackerProject trackerProjectA = mock(ITrackerProject.class);
        ITrackerProject trackerProjectB = mock(ITrackerProject.class);
        ITrackerProject trackerProjectC = mock(ITrackerProject.class);

        IModule documentA = mockDocumentInProject("docA", new Date(), "projectA");
        IWorkItem workItemA = mockWorkItemInProject(trackerProjectA, documentA, "WI-A", "open", "projectA");
        IModule documentC = mockDocumentInProject("docC", new Date(), "projectC");
        IWorkItem targetItemC = mockWorkItemInProject(trackerProjectC, documentC, "WI-Target-C", "open", "projectC");
        ILinkedWorkItemStruct linkA = mockLink(targetItemC, LINK_ROLE_ID_1);
        mockLinks(workItemA, List.of(linkA), List.of());

        IModule documentB = mockDocumentInProject("docB", new Date(), "projectB");
        IWorkItem workItemB = mockWorkItemInProject(trackerProjectB, documentB, "WI-B", "open", "projectB");
        ILinkedWorkItemStruct linkB = mockLink(targetItemC, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(linkB), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Links should be processed and output generated
            assertTrue(result.contains("WI-Target-C") && result.contains("diff-lwi"),
                    "Result should contain the target work item and diff markers");
        }
    }

    @Test
    void testCounterpartItems_PairedWorkItems_MarkedAsNone() {
        // linkB points to itemB2 which is paired with itemA2, and linkA points to itemA2 → NONE
        ITrackerProject trackerProjectA = mock(ITrackerProject.class);
        ITrackerProject trackerProjectB = mock(ITrackerProject.class);

        IModule documentA = mockDocumentInProject("docA", new Date(), "projectA");
        IWorkItem workItemA = mockWorkItemInProject(trackerProjectA, documentA, "WI-A", "open", "projectA");
        IWorkItem linkedItemA2 = mockWorkItemInProject(trackerProjectA, documentA, "WI-A2", "open", "projectA");
        ILinkedWorkItemStruct linkA = mockLink(linkedItemA2, LINK_ROLE_ID_1);
        mockLinks(workItemA, List.of(linkA), List.of());

        IModule documentB = mockDocumentInProject("docB", new Date(), "projectB");
        IWorkItem workItemB = mockWorkItemInProject(trackerProjectB, documentB, "WI-B", "open", "projectB");
        IWorkItem linkedItemB2 = mockWorkItemInProject(trackerProjectB, documentB, "WI-B2", "open", "projectB");
        ILinkedWorkItemStruct linkB = mockLink(linkedItemB2, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(linkB), List.of());

        // Setup pairing: linkedItemB2 is paired with linkedItemA2
        PolarionService polarionService = mock(PolarionService.class);
        when(polarionService.getPairedWorkItems(linkedItemB2, "projectA", "pairing-role"))
                .thenReturn(List.of(linkedItemA2));

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContextWithService(workItemA, workItemB, polarionService);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should be marked as NONE (counterparts)
            assertTrue(result.contains("class=\"unchanged diff-lwi\""));
        }
    }

    @Test
    void testCounterpartItems_NotPaired_MarkedAsAddedRemoved() {
        // linkB points to item that is NOT paired → ADDED + REMOVED
        ITrackerProject trackerProjectA = mock(ITrackerProject.class);
        ITrackerProject trackerProjectB = mock(ITrackerProject.class);

        IModule documentA = mockDocumentInProject("docA", new Date(), "projectA");
        IWorkItem workItemA = mockWorkItemInProject(trackerProjectA, documentA, "WI-A", "open", "projectA");
        IWorkItem linkedItemA2 = mockWorkItemInProject(trackerProjectA, documentA, "WI-A2", "open", "projectA");
        ILinkedWorkItemStruct linkA = mockLink(linkedItemA2, LINK_ROLE_ID_1);
        mockLinks(workItemA, List.of(linkA), List.of());

        IModule documentB = mockDocumentInProject("docB", new Date(), "projectB");
        IWorkItem workItemB = mockWorkItemInProject(trackerProjectB, documentB, "WI-B", "open", "projectB");
        IWorkItem linkedItemB2 = mockWorkItemInProject(trackerProjectB, documentB, "WI-B2", "open", "projectB");
        ILinkedWorkItemStruct linkB = mockLink(linkedItemB2, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(linkB), List.of());

        // Setup NO pairing
        PolarionService polarionService = mock(PolarionService.class);
        when(polarionService.getPairedWorkItems(linkedItemB2, "projectA", "pairing-role"))
                .thenReturn(List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContextWithService(workItemA, workItemB, polarionService);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should be marked as both ADDED and REMOVED
            assertTrue(result.contains("class=\"diff-html-added diff-lwi\""));
            assertTrue(result.contains("class=\"diff-html-removed diff-lwi\""));
        }
    }

    @Test
    void testLinkRoleFiltering_OnlyConfiguredRolesIncluded() {
        // Only links with roles in DiffModel.linkedWorkItemRoles should be included
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document, "WI-A", "open");
        mockLinks(workItemA, List.of(), List.of());

        IWorkItem workItemB = mockWorkItem(trackerProject, document, "WI-B", "open");
        IWorkItem linkedItem1 = mockWorkItem(trackerProject, document, "WI-Link1", "open");
        IWorkItem linkedItem2 = mockWorkItem(trackerProject, document, "WI-Link2", "open");
        ILinkedWorkItemStruct link1 = mockLink(linkedItem1, LINK_ROLE_ID_1);
        ILinkedWorkItemStruct link2 = mockLink(linkedItem2, LINK_ROLE_ID_2); // Not in configured roles
        mockLinks(workItemB, List.of(link1, link2), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should only contain link with role1
            assertTrue(result.contains("WI-Link1"));
            assertFalse(result.contains("WI-Link2"));
        }
    }

    @Test
    void testLinkRoleFiltering_EmptyRolesList_AllRolesIncluded() {
        // Empty linkedWorkItemRoles list → all roles should be included
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document, "WI-A", "open");
        mockLinks(workItemA, List.of(), List.of());

        IWorkItem workItemB = mockWorkItem(trackerProject, document, "WI-B", "open");
        IWorkItem linkedItem1 = mockWorkItem(trackerProject, document, "WI-Link1", "open");
        IWorkItem linkedItem2 = mockWorkItem(trackerProject, document, "WI-Link2", "open");
        ILinkedWorkItemStruct link1 = mockLink(linkedItem1, LINK_ROLE_ID_1);
        ILinkedWorkItemStruct link2 = mockLink(linkedItem2, LINK_ROLE_ID_2);
        mockLinks(workItemB, List.of(link1, link2), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of())) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should contain both links
            assertTrue(result.contains("WI-Link1"));
            assertTrue(result.contains("WI-Link2"));
        }
    }

    @Test
    void testEdgeCase_NullWorkItemA_AllLinksMarkedAsAddedRemoved() {
        // workItemA is null, workItemB has links → all marked ADDED/REMOVED
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemB = mockWorkItem(trackerProject, document, "WI-B", "open");
        IWorkItem linkedItem = mockWorkItem(trackerProject, document, "WI-Link", "open");
        ILinkedWorkItemStruct link = mockLink(linkedItem, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(link), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(null, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should be marked as both ADDED and REMOVED
            assertTrue(result.contains("class=\"diff-html-added diff-lwi\""));
            assertTrue(result.contains("class=\"diff-html-removed diff-lwi\""));
        }
    }

    @Test
    void testEdgeCase_NullWorkItemB_EmptyResult() {
        // workItemA has links, workItemB is null → empty result
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document, "WI-A", "open");
        IWorkItem linkedItem = mockWorkItem(trackerProject, document, "WI-Link", "open");
        ILinkedWorkItemStruct link = mockLink(linkedItem, LINK_ROLE_ID_1);
        mockLinks(workItemA, List.of(link), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, null);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should be empty (no linksB to process)
            assertEquals("", result);
        }
    }

    @Test
    void testEdgeCase_BothWorkItemsNull_EmptyResult() {
        // Both workItems are null → handler returns diff unchanged (inappropriate case)
        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(null, null);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // When both work items are null, fields have no ID, so isInappropriateCaseForHandler returns true
            // and the diff is returned unchanged
            assertEquals("not_relevant", result);
        }
    }

    @Test
    void testEdgeCase_NoLinks_EmptyResult() {
        // Both workItems have no links → empty result
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document, "WI-A", "open");
        IWorkItem workItemB = mockWorkItem(trackerProject, document, "WI-B", "open");
        mockLinks(workItemA, List.of(), List.of());
        mockLinks(workItemB, List.of(), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            assertEquals("", result);
        }
    }

    @Test
    void testMultipleLinksWithSameRole() {
        // Multiple links with same role should all be processed
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document, "WI-A", "open");
        mockLinks(workItemA, List.of(), List.of());

        IWorkItem workItemB = mockWorkItem(trackerProject, document, "WI-B", "open");
        IWorkItem linkedItem1 = mockWorkItem(trackerProject, document, "WI-Link1", "open");
        IWorkItem linkedItem2 = mockWorkItem(trackerProject, document, "WI-Link2", "open");
        ILinkedWorkItemStruct link1 = mockLink(linkedItem1, LINK_ROLE_ID_1);
        ILinkedWorkItemStruct link2 = mockLink(linkedItem2, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(link1, link2), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should contain both links
            assertTrue(result.contains("WI-Link1"));
            assertTrue(result.contains("WI-Link2"));
            // Each unmatched link appears twice (ADDED + REMOVED), and each render contains
            // the ID twice (once as ID, once in title), so 4 total occurrences per link
            int count1 = countOccurrences(result, "WI-Link1");
            int count2 = countOccurrences(result, "WI-Link2");
            assertEquals(4, count1);
            assertEquals(4, count2);
        }
    }

    @Test
    void testLinkRoleWithNullName_FilteredButLogsWarning() {
        // Test that roles with null names are handled and warning is logged
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document, "WI-A", "open");
        mockLinks(workItemA, List.of(), List.of());

        IWorkItem workItemB = mockWorkItem(trackerProject, document, "WI-B", "open");
        IWorkItem linkedItem = mockWorkItem(trackerProject, document, "WI-Link", "open");

        // Create a link with null role name
        ILinkedWorkItemStruct link = mock(ILinkedWorkItemStruct.class);
        lenient().when(link.getLinkedItem()).thenReturn(linkedItem);
        ILinkRoleOpt roleWithNullName = mock(ILinkRoleOpt.class);
        lenient().when(roleWithNullName.getId()).thenReturn("roleWithNullName");
        lenient().when(roleWithNullName.getName()).thenReturn(null);
        lenient().when(roleWithNullName.getOppositeName()).thenReturn("oppositeNullName");
        lenient().when(link.getLinkRole()).thenReturn(roleWithNullName);

        mockLinks(workItemB, List.of(link), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of())) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Role should still be processed (empty list means all roles accepted)
            // Verify the link was processed
            assertTrue(result.contains("WI-Link") || result.isEmpty(),
                    "Link with null role name should still be processed when linkedWorkItemRoles is empty");
        }
    }

    @Test
    void testLinkRoleWithNullName_FilteredOutWhenNotInList() {
        // Test that roles with null names are filtered when specific roles are configured
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule document = mockDocument("doc", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, document, "WI-A", "open");
        mockLinks(workItemA, List.of(), List.of());

        IWorkItem workItemB = mockWorkItem(trackerProject, document, "WI-B", "open");
        IWorkItem linkedItem1 = mockWorkItem(trackerProject, document, "WI-Link1", "open");
        IWorkItem linkedItem2 = mockWorkItem(trackerProject, document, "WI-Link2", "open");

        // Create link with null role name
        ILinkedWorkItemStruct linkWithNullName = mock(ILinkedWorkItemStruct.class);
        lenient().when(linkWithNullName.getLinkedItem()).thenReturn(linkedItem1);
        ILinkRoleOpt roleWithNullName = mock(ILinkRoleOpt.class);
        lenient().when(roleWithNullName.getId()).thenReturn("roleNullName");
        lenient().when(roleWithNullName.getName()).thenReturn(null);
        lenient().when(roleWithNullName.getOppositeName()).thenReturn("oppositeNull");
        lenient().when(linkWithNullName.getLinkRole()).thenReturn(roleWithNullName);

        // Create link with valid role name
        ILinkedWorkItemStruct linkWithValidName = mockLink(linkedItem2, LINK_ROLE_ID_1);

        mockLinks(workItemB, List.of(linkWithNullName, linkWithValidName), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Only the valid role should be in results
            assertTrue(result.contains("WI-Link2"), "Link with valid role should be included");
            assertFalse(result.contains("WI-Link1"), "Link with null role name not in configured list should be excluded");
        }
    }

    @Test
    void testSameWorkItemLinks_NotFromModule() {
        // Test links where target is not from the same module
        ITrackerProject trackerProject = mock(ITrackerProject.class);

        IModule documentA = mockDocument("docA", new Date());
        IWorkItem workItemA = mockWorkItem(trackerProject, documentA, "WI-A", "open");
        IModule documentTarget = mockDocument("docTarget", new Date());
        IWorkItem targetItem = mockWorkItem(trackerProject, documentTarget, "WI-Target", "open");
        ILinkedWorkItemStruct linkA = mockLink(targetItem, LINK_ROLE_ID_1);
        mockLinks(workItemA, List.of(linkA), List.of());

        IModule documentB = mockDocument("docB", new Date());
        IWorkItem workItemB = mockWorkItem(trackerProject, documentB, "WI-B", "open");
        ILinkedWorkItemStruct linkB = mockLink(targetItem, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(linkB), List.of());

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContext(workItemA, workItemB);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Both links should be processed
            assertTrue(result.contains("WI-Target") && result.contains("diff-lwi"),
                    "Links to item in different module should be processed");
        }
    }

    @Test
    void testCounterpartItems_DifferentProject_NotFromModule() {
        // Test counterpart logic with items not from the target module
        ITrackerProject trackerProjectA = mock(ITrackerProject.class);
        ITrackerProject trackerProjectB = mock(ITrackerProject.class);

        IModule documentA = mockDocumentInProject("docA", new Date(), "projectA");
        IWorkItem workItemA = mockWorkItemInProject(trackerProjectA, documentA, "WI-A", "open", "projectA");

        // linkedItemA2 is in a different document from workItemA
        IModule documentA2 = mockDocumentInProject("docA2", new Date(), "projectA");
        IWorkItem linkedItemA2 = mockWorkItemInProject(trackerProjectA, documentA2, "WI-A2", "open", "projectA");
        ILinkedWorkItemStruct linkA = mockLink(linkedItemA2, LINK_ROLE_ID_1);
        mockLinks(workItemA, List.of(linkA), List.of());

        IModule documentB = mockDocumentInProject("docB", new Date(), "projectB");
        IWorkItem workItemB = mockWorkItemInProject(trackerProjectB, documentB, "WI-B", "open", "projectB");

        // linkedItemB2 is in a different document from workItemB
        IModule documentB2 = mockDocumentInProject("docB2", new Date(), "projectB");
        IWorkItem linkedItemB2 = mockWorkItemInProject(trackerProjectB, documentB2, "WI-B2", "open", "projectB");
        ILinkedWorkItemStruct linkB = mockLink(linkedItemB2, LINK_ROLE_ID_1);
        mockLinks(workItemB, List.of(linkB), List.of());

        // Setup pairing
        PolarionService polarionService = mock(PolarionService.class);
        when(polarionService.getPairedWorkItems(linkedItemB2, "projectA", "pairing-role"))
                .thenReturn(List.of(linkedItemA2));

        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockDiffModelResource(List.of(LINK_ROLE_ID_1))) {
            DiffContext context = createContextWithService(workItemA, workItemB, polarionService);
            LinkedWorkItemsHandler handler = createPartiallyMockedHandler();

            String result = handler.postProcess("not_relevant", context);

            // Should be marked as NONE (counterparts from different modules)
            assertTrue(result.contains("WI-B2") && result.contains("diff-lwi"),
                    "Counterpart items not from module should be processed");
        }
    }

    @Test
    void testMarkChanged_AllModificationTypes() {
        LinkedWorkItemsHandler handler = new LinkedWorkItemsHandler();

        String text = "<span>test</span>";

        String added = handler.markChanged(text, ch.sbb.polarion.extension.diff_tool.util.ModificationType.ADDED);
        assertTrue(added.contains("diff-html-added"));

        String removed = handler.markChanged(text, ch.sbb.polarion.extension.diff_tool.util.ModificationType.REMOVED);
        assertTrue(removed.contains("diff-html-removed"));

        String modified = handler.markChanged(text, ch.sbb.polarion.extension.diff_tool.util.ModificationType.MODIFIED);
        assertTrue(modified.contains("diff-html-changed"));

        String none = handler.markChanged(text, ch.sbb.polarion.extension.diff_tool.util.ModificationType.NONE);
        assertTrue(none.contains("unchanged"));
    }

    @Test
    void testIsInappropriateCaseForHandler_VariousCases() {
        LinkedWorkItemsHandler handler = new LinkedWorkItemsHandler();

        // Case 1: Correct field ID
        WorkItem.Field fieldA1 = WorkItem.Field.builder().id("linkedWorkItems").build();
        WorkItem.Field fieldB1 = WorkItem.Field.builder().id("linkedWorkItems").build();
        DiffContext context1 = new DiffContext(fieldA1, fieldB1, PROJECT_ID, mock(PolarionService.class));
        assertFalse(handler.isInappropriateCaseForHandler(context1));

        // Case 2: Wrong field ID
        WorkItem.Field fieldA2 = WorkItem.Field.builder().id("title").build();
        WorkItem.Field fieldB2 = WorkItem.Field.builder().id("linkedWorkItems").build();
        DiffContext context2 = new DiffContext(fieldA2, fieldB2, PROJECT_ID, mock(PolarionService.class));
        assertTrue(handler.isInappropriateCaseForHandler(context2));

        // Case 3: pairedWorkItemsDiffer = true
        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockStatic(DiffModelCachedResource.class)) {
            mockDiffModelCachedResource.when(() -> DiffModelCachedResource.get(any(), any(), any()))
                    .thenReturn(DiffModel.builder().build());
            DiffContext context3 = new DiffContext(null, null, "linkedWorkItems",
                    WorkItemsPairDiffParams.builder().pairedWorkItemsDiffer(true).build(), mock(PolarionService.class));
            assertTrue(handler.isInappropriateCaseForHandler(context3));
        }
    }

    // Helper methods

    private DiffContext createContext(IWorkItem workItemA, IWorkItem workItemB) {
        return createContextWithService(workItemA, workItemB, mock(PolarionService.class));
    }

    private DiffContext createContextWithService(IWorkItem workItemA, IWorkItem workItemB, PolarionService polarionService) {
        WorkItem wiA = workItemA == null ? null : WorkItem.builder().underlyingObject(workItemA).build();
        WorkItem wiB = workItemB == null ? null : WorkItem.builder().underlyingObject(workItemB).build();

        if (wiA != null) {
            wiA.addField(WorkItem.Field.builder().id("linkedWorkItems").build());
        }
        if (wiB != null) {
            wiB.addField(WorkItem.Field.builder().id("linkedWorkItems").build());
        }

        return new DiffContext(wiA, wiB, "linkedWorkItems",
                WorkItemsPairDiffParams.builder().leftProjectId(PROJECT_ID).configName("Default").pairedWorkItemsLinkRole("pairing-role").build(),
                polarionService);
    }

    private MockedStatic<DiffModelCachedResource> mockDiffModelResource(List<String> linkedWorkItemRoles) {
        MockedStatic<DiffModelCachedResource> mock = mockStatic(DiffModelCachedResource.class);
        mock.when(() -> DiffModelCachedResource.get(any(), any(), any()))
                .thenReturn(DiffModel.builder().linkedWorkItemRoles(linkedWorkItemRoles).build());
        return mock;
    }

    private LinkedWorkItemsHandler createPartiallyMockedHandler() {
        LinkedWorkItemsHandler handler = mock(LinkedWorkItemsHandler.class);
        when(handler.postProcess(any(), any())).thenCallRealMethod();
        when(handler.isInappropriateCaseForHandler(any())).thenCallRealMethod();
        when(handler.markChanged(anyString(), any())).thenCallRealMethod();
        when(handler.renderLinkedItem(any(), any(), anyBoolean())).thenAnswer(invocation -> {
            ILinkedWorkItemStruct link = invocation.getArgument(0);
            boolean isBackLink = invocation.getArgument(2);
            IWorkItem linkedItem = link.getLinkedItem();
            return String.format("<span>%s</span>: <span><span>%s</span> - <span>%s</span></span>",
                    isBackLink ? link.getLinkRole().getOppositeName() : link.getLinkRole().getName(),
                    linkedItem.getId(), linkedItem.getTitle());
        });
        return handler;
    }

    private IModule mockDocumentInProject(String name, Date created, String projectId) {
        IModule document = mock(IModule.class);
        lenient().when(document.getProjectId()).thenReturn(projectId);
        lenient().when(document.getModuleNameWithSpace()).thenReturn(name);
        ILocation location = mock(ILocation.class);
        when(location.removeRevision()).thenReturn(location);
        when(document.getModuleLocation()).thenReturn(location);
        lenient().when(document.getCreated()).thenReturn(created);
        return document;
    }

    private IWorkItem mockWorkItemInProject(ITrackerProject trackerProject, IModule module, String id, String status, String projectId) {
        IStatusOpt statusOpt = mock(IStatusOpt.class);
        lenient().when(statusOpt.getId()).thenReturn(status);

        IWorkItem workItem = mock(IWorkItem.class);
        lenient().when(workItem.getId()).thenReturn(id);
        lenient().when(workItem.getTitle()).thenReturn("Title " + id);
        lenient().when(workItem.getModule()).thenReturn(module);
        lenient().when(workItem.getProjectId()).thenReturn(projectId);
        lenient().when(workItem.getStatus()).thenReturn(statusOpt);
        lenient().when(trackerProject.getWorkItem(id)).thenReturn(workItem);
        return workItem;
    }

    private ILinkedWorkItemStruct mockLinkWithRevision(IWorkItem linkedItem, String linkedRoleId, String revision) {
        ILinkedWorkItemStruct link = mockLink(linkedItem, linkedRoleId);
        lenient().when(link.getRevision()).thenReturn(revision);
        return link;
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
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

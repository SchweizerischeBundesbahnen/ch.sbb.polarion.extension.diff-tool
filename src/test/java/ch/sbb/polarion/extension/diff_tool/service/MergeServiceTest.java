package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.location.ILocation;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("diff-tool")
public class MergeServiceTest {
    @Mock
    private PolarionService polarionService;
    @Mock
    private SettingsService settingsService;

    private MergeService mergeService;

    @BeforeEach
    void init() {
        mergeService = new MergeService(polarionService);
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
                Arguments.of(sameProject(), linksList(a1rev2role1), linksList(a1rev1role1), List.of(),
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
            }
        }

        if (testContext.polarionServiceConsumer != null) {
            testContext.polarionServiceConsumer.accept(polarionService);
        }

        MergeContext context = mock(MergeContext.class);
        lenient().when(context.getLinkRole()).thenReturn("role1");

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

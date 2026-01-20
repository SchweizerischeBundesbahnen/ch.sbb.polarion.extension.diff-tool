package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.spi.PObjectList;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentsMergeContextTest {

    private static final String LEFT_PROJECT_ID = "leftProject";
    private static final String RIGHT_PROJECT_ID = "rightProject";
    private static final String SPACE_ID = "space";
    private static final String LEFT_DOC_NAME = "leftDoc";
    private static final String RIGHT_DOC_NAME = "rightDoc";
    private static final String LINK_ROLE = "testRole";

    @Mock
    private PolarionService polarionService;

    @Mock
    private IModule leftModule;

    @Mock
    private IModule rightModule;

    @Mock
    private ILinkRoleOpt linkRoleOpt;

    @Mock
    private ITrackerProject trackerProject;

    @Mock
    private DiffModel diffModel;

    @Mock
    private ILocation leftLocation;

    @Mock
    private ILocation rightLocation;

    private DocumentIdentifier leftDocIdentifier;
    private DocumentIdentifier rightDocIdentifier;

    @BeforeEach
    void setUp() {
        leftDocIdentifier = DocumentIdentifier.builder()
                .projectId(LEFT_PROJECT_ID)
                .spaceId(SPACE_ID)
                .name(LEFT_DOC_NAME)
                .build();

        rightDocIdentifier = DocumentIdentifier.builder()
                .projectId(RIGHT_PROJECT_ID)
                .spaceId(SPACE_ID)
                .name(RIGHT_DOC_NAME)
                .build();

        lenient().when(polarionService.getModule(leftDocIdentifier)).thenReturn(leftModule);
        lenient().when(polarionService.getModule(rightDocIdentifier)).thenReturn(rightModule);
        lenient().when(leftModule.getModuleLocation()).thenReturn(leftLocation);
        lenient().when(rightModule.getModuleLocation()).thenReturn(rightLocation);
        lenient().when(leftLocation.getLocationPath()).thenReturn("space/leftDoc");
        lenient().when(rightLocation.getLocationPath()).thenReturn("space/rightDoc");
        lenient().when(leftModule.getLastRevision()).thenReturn("100");
        lenient().when(rightModule.getLastRevision()).thenReturn("200");
    }

    private void setupLinkRole() {
        when(rightModule.getProject()).thenReturn(trackerProject);
        when(polarionService.getLinkRoleById(LINK_ROLE, trackerProject)).thenReturn(linkRoleOpt);
    }

    @Test
    void testConstructorLeftToRight() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertEquals(MergeDirection.LEFT_TO_RIGHT, context.getDirection());
        assertEquals(LINK_ROLE, context.getLinkRole());
        assertEquals(diffModel, context.getDiffModel());
        assertNotNull(context.getMergeReport());
    }

    @Test
    void testConstructorRightToLeft() {
        when(leftModule.getProject()).thenReturn(trackerProject);
        when(polarionService.getLinkRoleById(LINK_ROLE, trackerProject)).thenReturn(linkRoleOpt);

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.RIGHT_TO_LEFT, LINK_ROLE, diffModel);

        assertEquals(MergeDirection.RIGHT_TO_LEFT, context.getDirection());
    }

    @Test
    void testConstructorThrowsExceptionForInvalidLinkRole() {
        when(rightModule.getProject()).thenReturn(trackerProject);
        when(polarionService.getLinkRoleById(LINK_ROLE, trackerProject)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new DocumentsMergeContext(polarionService, leftDocIdentifier, rightDocIdentifier,
                        MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel));

        assertTrue(exception.getMessage().contains(LINK_ROLE));
    }

    @Test
    void testGetSourceModuleLeftToRight() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertEquals(leftModule, context.getSourceModule());
    }

    @Test
    void testGetSourceModuleRightToLeft() {
        when(leftModule.getProject()).thenReturn(trackerProject);
        when(polarionService.getLinkRoleById(LINK_ROLE, trackerProject)).thenReturn(linkRoleOpt);

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.RIGHT_TO_LEFT, LINK_ROLE, diffModel);

        assertEquals(rightModule, context.getSourceModule());
    }

    @Test
    void testGetTargetModuleLeftToRight() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertEquals(rightModule, context.getTargetModule());
    }

    @Test
    void testGetTargetModuleRightToLeft() {
        when(leftModule.getProject()).thenReturn(trackerProject);
        when(polarionService.getLinkRoleById(LINK_ROLE, trackerProject)).thenReturn(linkRoleOpt);

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.RIGHT_TO_LEFT, LINK_ROLE, diffModel);

        assertEquals(leftModule, context.getTargetModule());
    }

    @Test
    void testGetTargetDocumentIdentifierLeftToRight() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertEquals(rightDocIdentifier, context.getTargetDocumentIdentifier());
    }

    @Test
    void testGetTargetDocumentIdentifierRightToLeft() {
        when(leftModule.getProject()).thenReturn(trackerProject);
        when(polarionService.getLinkRoleById(LINK_ROLE, trackerProject)).thenReturn(linkRoleOpt);

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.RIGHT_TO_LEFT, LINK_ROLE, diffModel);

        assertEquals(leftDocIdentifier, context.getTargetDocumentIdentifier());
    }

    @Test
    void testBindCounterpartItemLeftNull() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        WorkItemsPair pair = new WorkItemsPair();
        pair.setLeftWorkItem(null);
        pair.setRightWorkItem(WorkItem.builder().id("right1").build());

        WorkItem newItem = WorkItem.builder().id("new1").build();
        context.bindCounterpartItem(pair, newItem);

        assertEquals(newItem, pair.getLeftWorkItem());
        assertEquals("right1", pair.getRightWorkItem().getId());
    }

    @Test
    void testBindCounterpartItemRightNull() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        WorkItemsPair pair = new WorkItemsPair();
        pair.setLeftWorkItem(WorkItem.builder().id("left1").build());
        pair.setRightWorkItem(null);

        WorkItem newItem = WorkItem.builder().id("new1").build();
        context.bindCounterpartItem(pair, newItem);

        assertEquals("left1", pair.getLeftWorkItem().getId());
        assertEquals(newItem, pair.getRightWorkItem());
    }

    @Test
    void testGetAffectedModulesInitiallyEmpty() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertNotNull(context.getAffectedModules());
        assertTrue(context.getAffectedModules().isEmpty());
    }

    @Test
    void testAllowReferencedWorkItemMerge() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertFalse(context.isAllowedReferencedWorkItemMerge());

        context.setAllowReferencedWorkItemMerge(true);
        assertTrue(context.isAllowedReferencedWorkItemMerge());

        context.setAllowReferencedWorkItemMerge(false);
        assertFalse(context.isAllowedReferencedWorkItemMerge());
    }

    @Test
    void testPreserveComments() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertFalse(context.isPreserveComments());

        context.setPreserveComments(true);
        assertTrue(context.isPreserveComments());

        context.setPreserveComments(false);
        assertFalse(context.isPreserveComments());
    }

    @Test
    void testParentsPairedBothNull() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        IModule.IStructureNode sourceNode = mock(IModule.IStructureNode.class);
        IModule.IStructureNode targetNode = mock(IModule.IStructureNode.class);

        when(sourceNode.getParent()).thenReturn(null);
        when(targetNode.getParent()).thenReturn(null);

        assertTrue(context.parentsPaired(sourceNode, targetNode));
    }

    @Test
    void testGetPairedWorkItemNotFound() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IModule oppositeModule = mock(IModule.class);

        when(oppositeModule.getAllWorkItems()).thenReturn(new PObjectList(mock(IDataService.class), List.of()));

        IWorkItem result = context.getPairedWorkItem(sourceWorkItem, oppositeModule);

        assertNull(result);
    }

    @Test
    void testAffectedModuleBuilder() {
        IModule module = mock(IModule.class);
        IWorkItem workItem = mock(IWorkItem.class);
        IModule.IStructureNode parentNode = mock(IModule.IStructureNode.class);

        DocumentsMergeContext.AffectedModule affectedModule = DocumentsMergeContext.AffectedModule.builder()
                .module(module)
                .workItem(workItem)
                .parentNode(parentNode)
                .destinationIndex(5)
                .build();

        assertEquals(module, affectedModule.getModule());
        assertEquals(workItem, affectedModule.getWorkItem());
        assertEquals(parentNode, affectedModule.getParentNode());
        assertEquals(5, affectedModule.getDestinationIndex());
    }

    @Test
    void testSourceAndTargetDocumentReferencesLeftToRight() {
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertNotNull(context.getSourceDocumentReference());
        assertNotNull(context.getTargetDocumentReference());
    }

    @Test
    void testSourceAndTargetDocumentReferencesRightToLeft() {
        when(leftModule.getProject()).thenReturn(trackerProject);
        when(polarionService.getLinkRoleById(LINK_ROLE, trackerProject)).thenReturn(linkRoleOpt);

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftDocIdentifier, rightDocIdentifier,
                MergeDirection.RIGHT_TO_LEFT, LINK_ROLE, diffModel);

        assertNotNull(context.getSourceDocumentReference());
        assertNotNull(context.getTargetDocumentReference());
    }

    @Test
    void testDocumentIdentifierWithRevision() {
        DocumentIdentifier leftWithRevision = DocumentIdentifier.builder()
                .projectId(LEFT_PROJECT_ID)
                .spaceId(SPACE_ID)
                .name(LEFT_DOC_NAME)
                .revision("50")
                .build();

        lenient().when(polarionService.getModule(leftWithRevision)).thenReturn(leftModule);
        setupLinkRole();

        DocumentsMergeContext context = new DocumentsMergeContext(
                polarionService, leftWithRevision, rightDocIdentifier,
                MergeDirection.LEFT_TO_RIGHT, LINK_ROLE, diffModel);

        assertNotNull(context.getSourceDocumentReference());
    }
}

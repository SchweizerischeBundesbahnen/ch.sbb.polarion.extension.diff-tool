package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IRevision;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentsPairTest {

    @Test
    void testNoArgsConstructor() {
        DocumentsPair pair = new DocumentsPair();
        assertNull(pair.getLeftDocument());
        assertNull(pair.getRightDocument());
    }

    @Test
    void testAllArgsConstructor() {
        Document leftDoc = Document.builder().id("leftId").build();
        Document rightDoc = Document.builder().id("rightId").build();

        DocumentsPair pair = new DocumentsPair(leftDoc, rightDoc);

        assertEquals(leftDoc, pair.getLeftDocument());
        assertEquals(rightDoc, pair.getRightDocument());
    }

    @Test
    void testBuilder() {
        Document leftDoc = Document.builder().id("leftId").build();
        Document rightDoc = Document.builder().id("rightId").build();

        DocumentsPair pair = DocumentsPair.builder()
                .leftDocument(leftDoc)
                .rightDocument(rightDoc)
                .build();

        assertEquals(leftDoc, pair.getLeftDocument());
        assertEquals(rightDoc, pair.getRightDocument());
    }

    @Test
    void testOfWithBothModules() {
        IModule leftModule = createMockModule("leftProject", "leftProjectName", "leftFolder", "leftId", "leftTitle", "leftRevision", "leftHead", "leftPath");
        IModule rightModule = createMockModule("rightProject", "rightProjectName", "rightFolder", "rightId", "rightTitle", "rightRevision", "rightHead", "rightPath");

        DocumentsPair pair = DocumentsPair.of(leftModule, rightModule);

        assertNotNull(pair.getLeftDocument());
        assertNotNull(pair.getRightDocument());
        assertEquals("leftId", pair.getLeftDocument().getId());
        assertEquals("rightId", pair.getRightDocument().getId());
    }

    @Test
    void testOfWithNullRightModule() {
        IModule leftModule = createMockModule("leftProject", "leftProjectName", "leftFolder", "leftId", "leftTitle", "leftRevision", "leftHead", "leftPath");

        DocumentsPair pair = DocumentsPair.of(leftModule, null);

        assertNotNull(pair.getLeftDocument());
        assertNull(pair.getRightDocument());
        assertEquals("leftId", pair.getLeftDocument().getId());
    }

    @Test
    void testSetters() {
        DocumentsPair pair = new DocumentsPair();

        Document leftDoc = Document.builder().id("newLeftId").build();
        Document rightDoc = Document.builder().id("newRightId").build();

        pair.setLeftDocument(leftDoc);
        pair.setRightDocument(rightDoc);

        assertEquals(leftDoc, pair.getLeftDocument());
        assertEquals(rightDoc, pair.getRightDocument());
    }

    private IModule createMockModule(String projectId, String projectName, String folder, String id, String title, String revision, String headRevision, String locationPath) {
        IModule moduleMock = mock(IModule.class);
        when(moduleMock.getModuleFolder()).thenReturn(folder);
        when(moduleMock.getId()).thenReturn(id);
        when(moduleMock.getTitleOrName()).thenReturn(title);
        when(moduleMock.getRevision()).thenReturn(revision);

        IDataService dataServiceMock = mock(IDataService.class);
        when(moduleMock.getDataSvc()).thenReturn(dataServiceMock);
        IRevision headRevisionMock = mock(IRevision.class);
        when(dataServiceMock.getLastStorageRevision()).thenReturn(headRevisionMock);
        when(headRevisionMock.getName()).thenReturn(headRevision);

        ITrackerProject projectMock = mock(ITrackerProject.class);
        when(moduleMock.getProject()).thenReturn(projectMock);
        when(projectMock.getId()).thenReturn(projectId);
        when(projectMock.getName()).thenReturn(projectName);

        ILocation locationMock = mock(ILocation.class);
        when(moduleMock.getModuleLocation()).thenReturn(locationMock);
        when(locationMock.getLocationPath()).thenReturn(locationPath);

        return moduleMock;
    }
}

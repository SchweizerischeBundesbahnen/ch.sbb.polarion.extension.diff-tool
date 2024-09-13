package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IRevision;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTest {
    @Test
    void testCreationFromModule() {
        IModule moduleMock = mock(IModule.class);
        when(moduleMock.getModuleFolder()).thenReturn("module_folder");
        when(moduleMock.getId()).thenReturn("module_id");
        when(moduleMock.getTitleOrName()).thenReturn("module_title");
        when(moduleMock.getRevision()).thenReturn("module_revision");
        mockHeadRevision(moduleMock, "head_revision");

        ITrackerProject projectMock = mock(ITrackerProject.class);
        when(moduleMock.getProject()).thenReturn(projectMock);
        when(projectMock.getId()).thenReturn("project_id");
        when(projectMock.getName()).thenReturn("project_name");

        ILocation locationMock = mock(ILocation.class);
        when(moduleMock.getModuleLocation()).thenReturn(locationMock);
        when(locationMock.getLocationPath()).thenReturn("location_path");

        Document document = Document.from(moduleMock);

        assertEquals("project_id", document.getProjectId());
        assertEquals("project_name", document.getProjectName());
        assertEquals("module_folder", document.getSpaceId());
        assertEquals("module_id", document.getId());
        assertEquals("module_title", document.getTitle());
        assertEquals("module_revision", document.getRevision());
        assertEquals("head_revision", document.getHeadRevision());
        assertEquals("location_path", document.getLocationPath());
    }

    @Test
    void testGetNotDefaultSpaceName() {
        IModule moduleMock = mock(IModule.class);
        mockHeadRevision(moduleMock, "any");
        when(moduleMock.getModuleFolder()).thenReturn("module_folder");

        ITrackerProject projectMock = mock(ITrackerProject.class);
        when(moduleMock.getProject()).thenReturn(projectMock);

        ILocation locationMock = mock(ILocation.class);
        when(moduleMock.getModuleLocation()).thenReturn(locationMock);

        Document document = Document.from(moduleMock);
        assertEquals("module_folder", document.getSpaceName());
    }

    @Test
    void testGetDefaultSpaceName() {
        IModule moduleMock = mock(IModule.class);
        mockHeadRevision(moduleMock, "any");
        when(moduleMock.getModuleFolder()).thenReturn("_default");

        ITrackerProject projectMock = mock(ITrackerProject.class);
        when(moduleMock.getProject()).thenReturn(projectMock);

        ILocation locationMock = mock(ILocation.class);
        when(moduleMock.getModuleLocation()).thenReturn(locationMock);

        Document document = Document.from(moduleMock);
        assertEquals("Default Space", document.getSpaceName());
    }

    private void mockHeadRevision(IModule moduleMock, String revision) {
        IDataService dataServiceMock = mock(IDataService.class);
        when(moduleMock.getDataSvc()).thenReturn(dataServiceMock);
        IRevision headRevision = mock(IRevision.class);
        when(dataServiceMock.getLastStorageRevision()).thenReturn(headRevision);
        when(headRevision.getName()).thenReturn(revision);
    }
}

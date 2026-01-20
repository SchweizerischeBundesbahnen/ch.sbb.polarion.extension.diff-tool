package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkItemTest {
    @Test
    void testCreation() {
        IWorkItem wiMock = mock(IWorkItem.class);
        when(wiMock.getId()).thenReturn("wi_id");
        when(wiMock.getTitle()).thenReturn("wi_title");
        when(wiMock.getRevision()).thenReturn("wi_revision");
        when(wiMock.getProjectId()).thenReturn("wi_project_id");

        IModule moduleMock = mock(IModule.class);
        when(wiMock.getModule()).thenReturn(moduleMock);

        WorkItem workItem = WorkItem.of(wiMock, "wi_outline_number", false, false);

        assertEquals("wi_id", workItem.getId());
        assertEquals("wi_title", workItem.getTitle());
        assertEquals("wi_outline_number", workItem.getOutlineNumber());
        assertEquals("wi_revision", workItem.getRevision());
        assertEquals("wi_project_id", workItem.getProjectId());
        assertEquals(moduleMock, workItem.getModule());
    }

    @Test
    void testFields() {
        IWorkItem wiMock = mock(IWorkItem.class);
        WorkItem workItem = WorkItem.of(wiMock, "wi_outline_number", false, false);

        workItem.addField(WorkItem.Field.builder().id("field_id").name("field_name").build());

        assertEquals(1, workItem.getFields().size());
        assertNotNull(workItem.getField("field_id"));
        assertEquals("field_name", Objects.requireNonNull(workItem.getField("field_id")).getName());
    }

    @Test
    void testSameDocument() {
        IWorkItem wiMock1 = mock(IWorkItem.class);
        WorkItem workItem1 = WorkItem.of(wiMock1, "1", false, false);

        IWorkItem wiMock2 = mock(IWorkItem.class);
        WorkItem workItem2 = WorkItem.of(wiMock2, "2", false, false);

        assertFalse(workItem1.sameDocument(workItem2));

        wiMock1 = mock(IWorkItem.class);
        when(wiMock1.getProjectId()).thenReturn("project1");
        wiMock2 = mock(IWorkItem.class);
        when(wiMock2.getProjectId()).thenReturn("project1");
        workItem1 = WorkItem.of(wiMock1, "1", false, false);
        workItem2 = WorkItem.of(wiMock2, "2", false, false);

        assertFalse(workItem1.sameDocument(workItem2));

        Date createdDate = new Date();
        wiMock1 = mock(IWorkItem.class);
        when(wiMock1.getProjectId()).thenReturn("project1");
        IModule moduleMock1 = mock(IModule.class);
        when(moduleMock1.getCreated()).thenReturn(createdDate);
        when(wiMock1.getModule()).thenReturn(moduleMock1);
        wiMock2 = mock(IWorkItem.class);
        when(wiMock2.getProjectId()).thenReturn("project1");
        IModule moduleMock2 = mock(IModule.class);
        when(moduleMock2.getCreated()).thenReturn(createdDate);
        when(wiMock2.getModule()).thenReturn(moduleMock2);
        workItem1 = WorkItem.of(wiMock1, "1", false, false);
        workItem2 = WorkItem.of(wiMock2, "2", false, false);

        assertTrue(workItem1.sameDocument(workItem2));
    }
}

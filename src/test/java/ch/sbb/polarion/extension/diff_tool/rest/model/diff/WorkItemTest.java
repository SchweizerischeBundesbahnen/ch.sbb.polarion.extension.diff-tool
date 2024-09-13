package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        WorkItem workItem = WorkItem.of(wiMock, "wi_outline_number", false);

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
        WorkItem workItem = WorkItem.of(wiMock, "wi_outline_number", false);

        workItem.addField(WorkItem.Field.builder().id("field_id").name("field_name").build());

        assertNotNull(workItem.getField("field_id"));
        assertEquals("field_name", Objects.requireNonNull(workItem.getField("field_id")).getName());
    }
}

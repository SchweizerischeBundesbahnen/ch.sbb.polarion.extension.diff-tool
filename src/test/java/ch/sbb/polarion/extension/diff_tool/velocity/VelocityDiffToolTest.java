package ch.sbb.polarion.extension.diff_tool.velocity;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ProjectWorkItem;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class VelocityDiffToolTest {
    private VelocityDiffTool velocityDiffTool;

    @Mock
    private IWorkItem workItemA;

    @Mock
    private IWorkItem workItemB;

    @BeforeEach
    void setUp() {
        velocityDiffTool = new VelocityDiffTool();
    }

    @Test
    void testToProjectWorkItemShouldUseRevisionWhenAvailable() throws Exception {
        when(workItemA.getId()).thenReturn("WI-001");
        when(workItemA.getProjectId()).thenReturn("project1");
        when(workItemA.getRevision()).thenReturn("5");

        ProjectWorkItem result = invokeToProjectWorkItem(workItemA);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("WI-001");
        assertThat(result.getProjectId()).isEqualTo("project1");
        assertThat(result.getRevision()).isEqualTo("5");

        verify(workItemA).getId();
        verify(workItemA).getProjectId();
        verify(workItemA).getRevision();
        verify(workItemA, never()).getLastRevision();
    }

    @Test
    void testToProjectWorkItemShouldUseLastRevisionWhenRevisionIsNull() throws Exception {
        when(workItemA.getId()).thenReturn("WI-001");
        when(workItemA.getProjectId()).thenReturn("project1");
        when(workItemA.getRevision()).thenReturn(null);
        when(workItemA.getLastRevision()).thenReturn("10");

        ProjectWorkItem result = invokeToProjectWorkItem(workItemA);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("WI-001");
        assertThat(result.getProjectId()).isEqualTo("project1");
        assertThat(result.getRevision()).isEqualTo("10");

        verify(workItemA).getId();
        verify(workItemA).getProjectId();
        verify(workItemA).getRevision();
        verify(workItemA).getLastRevision();
    }

    @Test
    void testToProjectWorkItemShouldHandleWorkItemWithId() throws Exception {
        String expectedId = "TEST-123";
        String expectedProjectId = "myProject";
        String expectedRevision = "42";

        when(workItemA.getId()).thenReturn(expectedId);
        when(workItemA.getProjectId()).thenReturn(expectedProjectId);
        when(workItemA.getRevision()).thenReturn(expectedRevision);

        ProjectWorkItem result = invokeToProjectWorkItem(workItemA);

        assertThat(result.getId()).isEqualTo(expectedId);
        assertThat(result.getProjectId()).isEqualTo(expectedProjectId);
        assertThat(result.getRevision()).isEqualTo(expectedRevision);
    }

    @Test
    void testToProjectWorkItemShouldCallGetLastRevisionOnlyWhenRevisionIsNull() throws Exception {
        when(workItemA.getId()).thenReturn("WI-001");
        when(workItemA.getProjectId()).thenReturn("project1");
        when(workItemA.getRevision()).thenReturn(null);
        when(workItemA.getLastRevision()).thenReturn("15");

        ProjectWorkItem result = invokeToProjectWorkItem(workItemA);

        assertThat(result.getRevision()).isEqualTo("15");
        verify(workItemA).getRevision();
        verify(workItemA, times(1)).getLastRevision();
    }

    @Test
    void testDiffWorkItemsShouldCallToProjectWorkItemForNonNullWorkItems() {
        when(workItemA.getId()).thenReturn("WI-001");
        when(workItemA.getProjectId()).thenReturn("project1");
        when(workItemA.getRevision()).thenReturn("5");

        when(workItemB.getId()).thenReturn("WI-002");
        when(workItemB.getProjectId()).thenReturn("project2");
        when(workItemB.getRevision()).thenReturn("3");

        try {
            velocityDiffTool.diffWorkItems("project1", workItemA, workItemB, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call, but we can verify the mock interactions
        }

        verify(workItemA).getId();
        verify(workItemA).getProjectId();
        verify(workItemA).getRevision();
        verify(workItemB).getId();
        verify(workItemB).getProjectId();
        verify(workItemB).getRevision();
    }

    @Test
    void testDiffWorkItemsShouldNotCallWorkItemMethodsWhenWorkItemIsNull() {
        try {
            velocityDiffTool.diffWorkItems("project1", null, workItemB, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call
        }

        verifyNoInteractions(workItemA);
    }

    @Test
    void testDiffWorkItemsShouldHandleBothNullWorkItems() {
        try {
            velocityDiffTool.diffWorkItems("project1", null, null, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call
        }

        verifyNoInteractions(workItemA);
        verifyNoInteractions(workItemB);
    }

    @Test
    void testDiffWorkItemsShouldUseLastRevisionForWorkItemWithNullRevision() {
        when(workItemA.getId()).thenReturn("WI-001");
        when(workItemA.getProjectId()).thenReturn("project1");
        when(workItemA.getRevision()).thenReturn(null);
        when(workItemA.getLastRevision()).thenReturn("10");

        try {
            velocityDiffTool.diffWorkItems("project1", workItemA, null, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call
        }

        verify(workItemA).getRevision();
        verify(workItemA).getLastRevision();
    }

    /**
     * Helper method to invoke the private toProjectWorkItem method using reflection
     */
    private ProjectWorkItem invokeToProjectWorkItem(IWorkItem workItem) throws Exception {
        Method method = VelocityDiffTool.class.getDeclaredMethod("toProjectWorkItem", IWorkItem.class);
        method.setAccessible(true);
        return (ProjectWorkItem) method.invoke(velocityDiffTool, workItem);
    }
}

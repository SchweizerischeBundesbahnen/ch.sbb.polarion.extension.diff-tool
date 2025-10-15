package ch.sbb.polarion.extension.diff_tool.velocity;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ProjectWorkItem;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class VelocityDiffToolTest {
    private VelocityDiffTool velocityDiffTool;

    @Mock
    private IWorkItem leftWorkItem;

    @Mock
    private IWorkItem rightWorkItem;

    @BeforeEach
    void setUp() {
        velocityDiffTool = new VelocityDiffTool();
    }

    @Test
    void testToProjectWorkItemShouldUseRevisionWhenAvailable() {
        when(leftWorkItem.getId()).thenReturn("WI-001");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getRevision()).thenReturn("5");

        ProjectWorkItem result = velocityDiffTool.toProjectWorkItem(leftWorkItem);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("WI-001");
        assertThat(result.getProjectId()).isEqualTo("project1");
        assertThat(result.getRevision()).isEqualTo("5");

        verify(leftWorkItem).getId();
        verify(leftWorkItem).getProjectId();
        verify(leftWorkItem).getRevision();
        verify(leftWorkItem, never()).getLastRevision();
    }

    @Test
    void testToProjectWorkItemShouldUseLastRevisionWhenRevisionIsNull() {
        when(leftWorkItem.getId()).thenReturn("WI-001");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getRevision()).thenReturn(null);
        when(leftWorkItem.getLastRevision()).thenReturn("10");

        ProjectWorkItem result = velocityDiffTool.toProjectWorkItem(leftWorkItem);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("WI-001");
        assertThat(result.getProjectId()).isEqualTo("project1");
        assertThat(result.getRevision()).isEqualTo("10");

        verify(leftWorkItem).getId();
        verify(leftWorkItem).getProjectId();
        verify(leftWorkItem).getRevision();
        verify(leftWorkItem).getLastRevision();
    }

    @Test
    void testToProjectWorkItemShouldHandleWorkItemWithId() {
        String expectedId = "TEST-123";
        String expectedProjectId = "myProject";
        String expectedRevision = "42";

        when(leftWorkItem.getId()).thenReturn(expectedId);
        when(leftWorkItem.getProjectId()).thenReturn(expectedProjectId);
        when(leftWorkItem.getRevision()).thenReturn(expectedRevision);

        ProjectWorkItem result = velocityDiffTool.toProjectWorkItem(leftWorkItem);

        assertThat(result.getId()).isEqualTo(expectedId);
        assertThat(result.getProjectId()).isEqualTo(expectedProjectId);
        assertThat(result.getRevision()).isEqualTo(expectedRevision);
    }

    @Test
    void testToProjectWorkItemShouldCallGetLastRevisionOnlyWhenRevisionIsNull() {
        when(leftWorkItem.getId()).thenReturn("WI-001");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getRevision()).thenReturn(null);
        when(leftWorkItem.getLastRevision()).thenReturn("15");

        ProjectWorkItem result = velocityDiffTool.toProjectWorkItem(leftWorkItem);

        assertThat(result.getRevision()).isEqualTo("15");
        verify(leftWorkItem).getRevision();
        verify(leftWorkItem, times(1)).getLastRevision();
    }

    @Test
    void testDiffWorkItemsShouldCallToProjectWorkItemForNonNullWorkItems() {
        when(leftWorkItem.getId()).thenReturn("WI-001");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getRevision()).thenReturn("5");

        when(rightWorkItem.getId()).thenReturn("WI-002");
        when(rightWorkItem.getProjectId()).thenReturn("project2");
        when(rightWorkItem.getRevision()).thenReturn("3");

        try {
            velocityDiffTool.diffWorkItems("project1", leftWorkItem, rightWorkItem, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call, but we can verify the mock interactions
        }

        verify(leftWorkItem).getId();
        verify(leftWorkItem).getProjectId();
        verify(leftWorkItem).getRevision();
        verify(rightWorkItem).getId();
        verify(rightWorkItem).getProjectId();
        verify(rightWorkItem).getRevision();
    }

    @Test
    void testDiffWorkItemsShouldNotCallWorkItemMethodsWhenWorkItemIsNull() {
        try {
            velocityDiffTool.diffWorkItems("project1", null, rightWorkItem, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call
        }

        verifyNoInteractions(leftWorkItem);
    }

    @Test
    void testDiffWorkItemsShouldHandleBothNullWorkItems() {
        try {
            velocityDiffTool.diffWorkItems("project1", null, null, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call
        }

        verifyNoInteractions(leftWorkItem);
        verifyNoInteractions(rightWorkItem);
    }

    @Test
    void testDiffWorkItemsShouldUseLastRevisionForWorkItemWithNullRevision() {
        when(leftWorkItem.getId()).thenReturn("WI-001");
        when(leftWorkItem.getProjectId()).thenReturn("project1");
        when(leftWorkItem.getRevision()).thenReturn(null);
        when(leftWorkItem.getLastRevision()).thenReturn("10");

        try {
            velocityDiffTool.diffWorkItems("project1", leftWorkItem, null, "config", "parent");
        } catch (Exception e) {
            // Expected to fail due to real DiffService call
        }

        verify(leftWorkItem).getRevision();
        verify(leftWorkItem).getLastRevision();
    }
}

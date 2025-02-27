package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Project;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class WorkItemsMergeContextTest {
    private Project leftProject;
    private Project rightProject;
    private MergeDirection leftToRight;
    private MergeDirection rightToLeft;
    private DiffModel diffModel;
    private final String linkRole = "testRole";

    @BeforeEach
    void setUp() {
        leftProject = mock(Project.class);
        rightProject = mock(Project.class);
        diffModel = mock(DiffModel.class);
        leftToRight = MergeDirection.LEFT_TO_RIGHT;
        rightToLeft = MergeDirection.RIGHT_TO_LEFT;
    }

    @Test
    void testGetSourceProjectLeftToRight() {
        WorkItemsMergeContext context = new WorkItemsMergeContext(leftProject, rightProject, leftToRight, linkRole, diffModel);
        assertEquals(leftProject, context.getSourceProject());
    }

    @Test
    void testGetSourceProjectRightToLeft() {
        WorkItemsMergeContext context = new WorkItemsMergeContext(leftProject, rightProject, rightToLeft, linkRole, diffModel);
        assertEquals(rightProject, context.getSourceProject());
    }

    @Test
    void testGetTargetProjectLeftToRight() {
        WorkItemsMergeContext context = new WorkItemsMergeContext(leftProject, rightProject, leftToRight, linkRole, diffModel);
        assertEquals(rightProject, context.getTargetProject());
    }

    @Test
    void testGetTargetProjectRightToLeft() {
        WorkItemsMergeContext context = new WorkItemsMergeContext(leftProject, rightProject, rightToLeft, linkRole, diffModel);
        assertEquals(leftProject, context.getTargetProject());
    }
}

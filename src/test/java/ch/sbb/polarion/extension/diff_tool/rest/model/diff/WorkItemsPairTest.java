package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.service.CalculatePairsContext;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkItemsPairTest {
    @Test
    void testCreation() {
        IWorkItem wiMock1 = mock(IWorkItem.class);
        when(wiMock1.getId()).thenReturn("id1");
        IWorkItem wiMock2 = mock(IWorkItem.class);
        when(wiMock2.getId()).thenReturn("id2");

        CalculatePairsContext calculatePairsContext = mock(CalculatePairsContext.class);
        IModule leftDocument = mock(IModule.class);
        when(leftDocument.getProjectId()).thenReturn("projectId");
        IModule rightDocument = mock(IModule.class);
        when(rightDocument.getProjectId()).thenReturn("projectId");
        when(calculatePairsContext.getLeftDocument()).thenReturn(leftDocument);
        when(calculatePairsContext.getRightDocument()).thenReturn(rightDocument);

        WorkItemsPair workItemsPair = WorkItemsPair.of(Pair.of(wiMock1, wiMock2), calculatePairsContext);

        assertEquals("id1", workItemsPair.getLeftWorkItem().getId());
        assertEquals("id2", workItemsPair.getRightWorkItem().getId());
    }
}

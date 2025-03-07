package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.service.CalculatePairsContext;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkItemsPairDiffTest {
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

        workItemsPair.getLeftWorkItem().addField(WorkItem.Field.builder().id("id").htmlDiff("diffLeft").build());
        workItemsPair.getRightWorkItem().addField(WorkItem.Field.builder().id("id").htmlDiff("diffRight").build());

        Set<String> fieldIds = new HashSet<>();
        fieldIds.add("id");
        fieldIds.add("title");
        Collection<WorkItemsPairDiff.FieldDiff> fieldDiffs = WorkItemsPairDiff.of(workItemsPair, fieldIds).getFieldDiffs();

        assertEquals(1, fieldDiffs.size());

        WorkItemsPairDiff.FieldDiff fieldDiff = fieldDiffs.iterator().next();
        assertEquals("id", fieldDiff.getId());
        assertEquals("diffLeft", fieldDiff.getDiffLeft());
        assertEquals("diffRight", fieldDiff.getDiffRight());
    }
}

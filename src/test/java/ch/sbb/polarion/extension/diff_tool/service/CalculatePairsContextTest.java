package ch.sbb.polarion.extension.diff_tool.service;

import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalculatePairsContextTest {

    @Test
    void isSuitableLinkRoleReturnsFalseWhenLinkedByRoleIsNull() {
        IModule leftDocument = mock(IModule.class);
        when(leftDocument.getAllWorkItems()).thenReturn(Collections.emptyList());
        IModule rightDocument = mock(IModule.class);
        when(rightDocument.getAllWorkItems()).thenReturn(Collections.emptyList());

        CalculatePairsContext context = new CalculatePairsContext(leftDocument, rightDocument, null, Collections.emptyList());

        ILinkRoleOpt anyRole = mock(ILinkRoleOpt.class);
        assertFalse(context.isSuitableLinkRole(anyRole));
    }

    @Test
    void isSuitableLinkRoleMatchesById() {
        IModule leftDocument = mock(IModule.class);
        when(leftDocument.getAllWorkItems()).thenReturn(Collections.emptyList());
        IModule rightDocument = mock(IModule.class);
        when(rightDocument.getAllWorkItems()).thenReturn(Collections.emptyList());

        ILinkRoleOpt linkedByRole = mock(ILinkRoleOpt.class);
        when(linkedByRole.getId()).thenReturn("relates-to");
        CalculatePairsContext context = new CalculatePairsContext(leftDocument, rightDocument, linkedByRole, Collections.emptyList());

        ILinkRoleOpt sameId = mock(ILinkRoleOpt.class);
        when(sameId.getId()).thenReturn("relates-to");
        ILinkRoleOpt otherId = mock(ILinkRoleOpt.class);
        when(otherId.getId()).thenReturn("blocks");

        assertTrue(context.isSuitableLinkRole(sameId));
        assertFalse(context.isSuitableLinkRole(otherId));
    }

    @Test
    void branchedDocumentsFlagDefaultsToFalseAndIsExposed() {
        IModule leftDocument = mock(IModule.class);
        when(leftDocument.getAllWorkItems()).thenReturn(Collections.emptyList());
        IModule rightDocument = mock(IModule.class);
        when(rightDocument.getAllWorkItems()).thenReturn(Collections.emptyList());

        assertFalse(new CalculatePairsContext(leftDocument, rightDocument, null, Collections.emptyList()).isBranchedDocuments());
        assertTrue(new CalculatePairsContext(leftDocument, rightDocument, true, null, Collections.emptyList()).isBranchedDocuments());
    }

    @Test
    void getOutlineNumberUsesSeparateCachesPerDocumentScope() {
        // The same work item may reside in both branched documents but with a different outline number in each,
        // so the outline number must be cached per document scope, not globally.
        IModule leftDocument = mock(IModule.class);
        when(leftDocument.getAllWorkItems()).thenReturn(Collections.emptyList());
        IModule rightDocument = mock(IModule.class);
        when(rightDocument.getAllWorkItems()).thenReturn(Collections.emptyList());

        IWorkItem workItem = mock(IWorkItem.class);
        when(leftDocument.getOutlineNumberOfWorkitem(workItem)).thenReturn("1");
        when(rightDocument.getOutlineNumberOfWorkitem(workItem)).thenReturn("2");

        CalculatePairsContext context = new CalculatePairsContext(leftDocument, rightDocument, null, Collections.emptyList());

        assertEquals("1", context.getOutlineNumber(workItem, true));
        assertEquals("2", context.getOutlineNumber(workItem, false));
        // Second lookups are served from the per-scope caches, so the modules are queried only once each
        assertEquals("1", context.getOutlineNumber(workItem, true));
        assertEquals("2", context.getOutlineNumber(workItem, false));
        verify(leftDocument, times(1)).getOutlineNumberOfWorkitem(workItem);
        verify(rightDocument, times(1)).getOutlineNumberOfWorkitem(workItem);
    }
}

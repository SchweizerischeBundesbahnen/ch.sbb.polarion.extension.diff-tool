package ch.sbb.polarion.extension.diff_tool.service;

import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
}

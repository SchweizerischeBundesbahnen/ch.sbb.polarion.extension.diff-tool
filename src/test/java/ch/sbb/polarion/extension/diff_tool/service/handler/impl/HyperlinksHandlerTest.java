package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import com.polarion.alm.tracker.internal.model.HyperlinkStruct;
import com.polarion.alm.tracker.model.IHyperlinkRoleOpt;
import com.polarion.alm.tracker.model.ITypeOpt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HyperlinksHandlerTest {

    @Test
    void testPositiveCase() {
        DiffContext context = mockContext();

        String result = new HyperlinksHandler().postProcess("test", context);

        // The source-only link urlA2 is removed and the target-only links are added, each shown once
        assertEquals(row("unchanged", "urlA1") + row("diff-html-added", "urlB1")
                + row("diff-html-removed", "urlA2") + row("diff-html-added", "urlB2"), result);
    }

    @Test
    void testReversedDiffSwapsRemovedAndAdded() {
        DiffContext context = mockContext();
        when(context.reverseStyles()).thenReturn(true);

        String result = new HyperlinksHandler().postProcess("test", context);

        assertEquals(row("unchanged", "urlA1") + row("diff-html-removed", "urlB1")
                + row("diff-html-removed", "urlB2") + row("diff-html-added", "urlA2"), result);
    }

    private DiffContext mockContext() {
        DiffContext context = mock(DiffContext.class, RETURNS_DEEP_STUBS);

        context.setFieldA(mock(WorkItem.Field.class, RETURNS_DEEP_STUBS));
        when(context.getFieldA().getId()).thenReturn("hyperlinks");

        context.setFieldB(mock(WorkItem.Field.class, RETURNS_DEEP_STUBS));
        when(context.getFieldB().getId()).thenReturn("hyperlinks");

        context.setWorkItemA(mock(WorkItem.class, RETURNS_DEEP_STUBS));
        context.setWorkItemB(mock(WorkItem.class, RETURNS_DEEP_STUBS));

        context.setDiffModel(mock(DiffModel.class, RETURNS_DEEP_STUBS));

        ITypeOpt typeA = mock(ITypeOpt.class);
        when(typeA.getId()).thenReturn("someType");
        when(context.getWorkItemA().getUnderlyingObject().getType()).thenReturn(typeA);

        List<HyperlinkStruct> hyperlinksA = List.of(
                mockHyperlinkStruct("urlA1", "role1"),
                mockHyperlinkStruct("urlA2", "role2"),
                mockHyperlinkStruct("urlA3", "role3"),
                mockHyperlinkStruct("urlA4", null)
        );
        List<HyperlinkStruct> hyperlinksB = List.of(
                mockHyperlinkStruct("urlA1", "role1"),
                mockHyperlinkStruct("urlB1", "role1"),
                mockHyperlinkStruct("urlB2", "role2"),
                mockHyperlinkStruct("urlB3", "role3"),
                mockHyperlinkStruct("urlB4", null)
        );
        when(context.getWorkItemA().getUnderlyingObject().getHyperlinks()).thenReturn(hyperlinksA);
        when(context.getWorkItemB().getUnderlyingObject().getHyperlinks()).thenReturn(hyperlinksB);

        when(context.getDiffModel().getHyperlinkRoles()).thenReturn(List.of("someType#role1", "someType#role2"));
        return context;
    }

    private String row(String styleName, String uri) {
        return """
                <div class="diff-hl-container"><div class="%1$s diff-hl"><span class="polarion-JSEnumOption" title="external reference">null</span>:
                <a href="%2$s" target="_blank" class="polarion-Hyperlink">
                    <span class="polarion-JSTextRenderer-Text" title="%2$s">%2$s</span>
                </a></div></div>""".formatted(styleName, uri);
    }

    private HyperlinkStruct mockHyperlinkStruct(String uri, String roleId) {
        HyperlinkStruct mockedStruct = mock(HyperlinkStruct.class);
        when(mockedStruct.getUri()).thenReturn(uri);
        if (roleId != null) {
            IHyperlinkRoleOpt roleOpt = mock(IHyperlinkRoleOpt.class);
            when(roleOpt.getId()).thenReturn(roleId);
            when(mockedStruct.getRole()).thenReturn(roleOpt);
        }
        return mockedStruct;
    }

}

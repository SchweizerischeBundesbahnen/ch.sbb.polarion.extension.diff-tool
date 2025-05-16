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
        String input = "test";
        DiffContext context = mock(DiffContext.class, RETURNS_DEEP_STUBS);

        context.fieldA = mock(WorkItem.Field.class, RETURNS_DEEP_STUBS);
        when(context.fieldA.getId()).thenReturn("hyperlinks");

        context.fieldB = mock(WorkItem.Field.class, RETURNS_DEEP_STUBS);
        when(context.fieldB.getId()).thenReturn("hyperlinks");

        context.workItemA = mock(WorkItem.class, RETURNS_DEEP_STUBS);
        context.workItemB = mock(WorkItem.class, RETURNS_DEEP_STUBS);

        context.diffModel = mock(DiffModel.class, RETURNS_DEEP_STUBS);

        ITypeOpt typeA = mock(ITypeOpt.class);
        when(typeA.getId()).thenReturn("someType");
        when(context.workItemA.getUnderlyingObject().getType()).thenReturn(typeA);

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
        when(context.workItemA.getUnderlyingObject().getHyperlinks()).thenReturn(hyperlinksA);
        when(context.workItemB.getUnderlyingObject().getHyperlinks()).thenReturn(hyperlinksB);

        when(context.diffModel.getHyperlinkRoles()).thenReturn(List.of("someType#role1", "someType#role2"));

        String result = new HyperlinksHandler().postProcess(input, context);

        String expectedResult = """
                <div class="diff-hl-container"><div class="unchanged diff-hl"><span class="polarion-JSEnumOption" title="external reference">null</span>:
                <a href="urlA1" target="_blank" class="polarion-Hyperlink">
                    <span class="polarion-JSTextRenderer-Text" title="urlA1">urlA1</span>
                </a></div></div><div class="diff-hl-container"><div class="diff-html-added diff-hl"><span class="polarion-JSEnumOption" title="external reference">null</span>:
                <a href="urlB1" target="_blank" class="polarion-Hyperlink">
                    <span class="polarion-JSTextRenderer-Text" title="urlB1">urlB1</span>
                </a></div></div><div class="diff-hl-container"><div class="diff-html-removed diff-hl"><span class="polarion-JSEnumOption" title="external reference">null</span>:
                <a href="urlB1" target="_blank" class="polarion-Hyperlink">
                    <span class="polarion-JSTextRenderer-Text" title="urlB1">urlB1</span>
                </a></div></div><div class="diff-hl-container"><div class="diff-html-added diff-hl"><span class="polarion-JSEnumOption" title="external reference">null</span>:
                <a href="urlB2" target="_blank" class="polarion-Hyperlink">
                    <span class="polarion-JSTextRenderer-Text" title="urlB2">urlB2</span>
                </a></div></div><div class="diff-hl-container"><div class="diff-html-removed diff-hl"><span class="polarion-JSEnumOption" title="external reference">null</span>:
                <a href="urlB2" target="_blank" class="polarion-Hyperlink">
                    <span class="polarion-JSTextRenderer-Text" title="urlB2">urlB2</span>
                </a></div></div>""";
        assertEquals(expectedResult, result);
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

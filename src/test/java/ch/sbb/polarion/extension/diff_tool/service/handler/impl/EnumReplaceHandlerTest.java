package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import com.polarion.subterra.base.data.model.IEnumType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnumReplaceHandlerTest {
    private static final String STATUS = "status";

    private EnumReplaceHandler handler;

    @Mock
    private WorkItem workItemA;

    @Mock
    private WorkItem workItemB;

    @BeforeEach
    public void init() {
        handler = new EnumReplaceHandler();
    }

    @Test
    void testHandler() {
        when(workItemA.getField(STATUS)).thenReturn(WorkItem.Field.builder()
                .id(STATUS)
                .html("<span class=\"polarion-JSEnumOption\" title=\"Draft\"><img style=\"vertical-align:bottom;border:0px;margin-right:2px;\" src=\"/polarion/icons/default/enums/req_status_draft.gif?buildId=20231023-1350-2310-dd0c1db0\" md5=\"80ab751a2fe3b3e5e69c0e9e725a8aca\"\" alt=\"\"/>Draft</span>")
                .type(mock(IEnumType.class))
                .build());
        when(workItemB.getField(STATUS)).thenReturn(WorkItem.Field.builder()
                .id(STATUS)
                .html("<span class=\"polarion-JSEnumOption\" title=\"Open\"><img style=\"vertical-align:bottom;border:0px;margin-right:2px;\" src=\"/polarion/icons/default/enums/status_open.gif?buildId=20231023-1350-2310-dd0c1db0\" alt=\"\"/>Open</span>")
                .type(mock(IEnumType.class))
                .build());

        DiffContext context = new DiffContext(workItemA, workItemB, STATUS, WorkItemsPairDiffParams.builder().build(), mock(PolarionService.class));

        Pair<String, String> preProcessed = handler.preProcess(Pair.of(workItemA.getField(STATUS).getHtml(), workItemB.getField(STATUS).getHtml()), context); //NOSONAR
        assertEquals("LeftTokenToReplace", preProcessed.getLeft());
        assertEquals("RightTokenToReplace", preProcessed.getRight());

        assertEquals("<span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\"><span class=\"polarion-JSEnumOption\" title=\"Draft\"><img style=\"vertical-align:bottom;border:0px;margin-right:2px;\" src=\"/polarion/icons/default/enums/req_status_draft.gif?buildId=20231023-1350-2310-dd0c1db0\" md5=\"80ab751a2fe3b3e5e69c0e9e725a8aca\"\" alt=\"\"/>Draft</span></span><span class=\"diff-html-added\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeId=\"added-diff-0\" next=\"last-diff\"><span class=\"polarion-JSEnumOption\" title=\"Open\"><img style=\"vertical-align:bottom;border:0px;margin-right:2px;\" src=\"/polarion/icons/default/enums/status_open.gif?buildId=20231023-1350-2310-dd0c1db0\" alt=\"\"/>Open</span></span>",
                handler.postProcess("<span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\">LeftTokenToReplace</span><span class=\"diff-html-added\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeId=\"added-diff-0\" next=\"last-diff\">RightTokenToReplace</span>", context));
    }
}

package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.DiffToolRestApplication;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.DiffService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueService;
import ch.sbb.polarion.extension.diff_tool.service.queue.FeatureExecutionTask;
import ch.sbb.polarion.extension.diff_tool.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import jakarta.ws.rs.BadRequestException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DiffInternalControllerTest {

    private DiffInternalController controller;
    private DiffService diffService;
    private MockedConstruction<PolarionService> polarionServiceConstruction;
    private MockedStatic<DiffToolRestApplication> mockedApp;

    @BeforeEach
    void setUp() throws Exception {
        polarionServiceConstruction = TestUtils.mockPolarionServiceConstruction();

        ExecutionQueueService queue = mock(ExecutionQueueService.class);
        lenient().when(queue.executeAndWait(any(FeatureExecutionTask.class))).thenAnswer(inv -> {
            FeatureExecutionTask<?> task = inv.getArgument(0);
            return task.getTask().call();
        });

        mockedApp = mockStatic(DiffToolRestApplication.class);
        mockedApp.when(DiffToolRestApplication::getExecutionService).thenReturn(queue);

        controller = new DiffInternalController();

        diffService = mock(DiffService.class);
        Field diffServiceField = DiffInternalController.class.getDeclaredField("diffService");
        diffServiceField.setAccessible(true);
        diffServiceField.set(controller, diffService);
    }

    @AfterEach
    void tearDown() {
        polarionServiceConstruction.close();
        mockedApp.close();
    }

    @Test
    void getDocumentsDiffThrowsWhenParamsNull() {
        assertThrows(BadRequestException.class, () -> controller.getDocumentsDiff(null));
    }

    @Test
    void getDocumentsDiffThrowsWhenLeftDocumentMissing() {
        DocumentsDiffParams params = new DocumentsDiffParams(null, mock(DocumentIdentifier.class), "role", null, null);
        assertThrows(BadRequestException.class, () -> controller.getDocumentsDiff(params));
    }

    @Test
    void getDocumentsDiffThrowsWhenRightDocumentMissing() {
        DocumentsDiffParams params = new DocumentsDiffParams(mock(DocumentIdentifier.class), null, "role", null, null);
        assertThrows(BadRequestException.class, () -> controller.getDocumentsDiff(params));
    }

    @Test
    void getDocumentsDiffThrowsWhenLinkRoleMissingForDifferentDocuments() {
        DocumentIdentifier left = DocumentIdentifier.builder().projectId("p").spaceId("s").name("a").build();
        DocumentIdentifier right = DocumentIdentifier.builder().projectId("p").spaceId("s").name("b").build();
        DocumentsDiffParams params = new DocumentsDiffParams(left, right, null, null, null);
        assertThrows(BadRequestException.class, () -> controller.getDocumentsDiff(params));
    }

    @Test
    void getDocumentsDiffAllowsMissingLinkRoleWhenSameDocument() {
        DocumentIdentifier left = DocumentIdentifier.builder().projectId("p").spaceId("s").name("doc").revision("r1").build();
        DocumentIdentifier right = DocumentIdentifier.builder().projectId("p").spaceId("s").name("doc").revision("r2").build();
        DocumentsDiffParams params = new DocumentsDiffParams(left, right, null, null, null);

        DocumentsDiff expected = DocumentsDiff.builder().build();
        when(diffService.getDocumentsDiff(any(), any(), any(), any(), any())).thenReturn(expected);

        assertNotNull(controller.getDocumentsDiff(params));
    }

    @Test
    void getDocumentsDiffPassesValidLinkRoleThrough() {
        DocumentIdentifier left = DocumentIdentifier.builder().projectId("pA").spaceId("s").name("doc").build();
        DocumentIdentifier right = DocumentIdentifier.builder().projectId("pB").spaceId("s").name("doc").build();
        DocumentsDiffParams params = new DocumentsDiffParams(left, right, "relates-to", null, null);

        DocumentsDiff expected = DocumentsDiff.builder().build();
        when(diffService.getDocumentsDiff(any(), any(), any(), any(), any())).thenReturn(expected);

        assertNotNull(controller.getDocumentsDiff(params));
    }
}

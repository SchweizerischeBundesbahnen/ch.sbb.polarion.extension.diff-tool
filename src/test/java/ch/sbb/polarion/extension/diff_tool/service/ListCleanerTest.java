package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.service.cleaners.ListCleanerProvider;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IComment;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkRecord;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignature;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignaturesManager;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.spi.PObjectList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListCleanerTest {

    @Test
    void testApprovalsCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IUser user = mock(IUser.class);
        IApprovalStruct approvalStruct = mock(IApprovalStruct.class);
        when(approvalStruct.getUser()).thenReturn(user);
        when(workItem.getApprovals()).thenReturn(List.of(approvalStruct));

        ListCleanerProvider.getInstance(IWorkItem.KEY_APPROVALS).clean(workItem);

        verify(workItem, times(1)).removeApprovee(user);
    }

    @Test
    void testAssigneeCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IUser user = mock(IUser.class);
        when(workItem.getAssignees()).thenReturn(new PObjectList(mock(IDataService.class), List.of(user)));

        ListCleanerProvider.getInstance(IWorkItem.KEY_ASSIGNEE).clean(workItem);

        verify(workItem, times(1)).removeAssignee(user);
    }

    @Test
    void testCommentsCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IComment comment = mock(IComment.class);
        IDataService dataService = mock(IDataService.class);
        when(workItem.getDataSvc()).thenReturn(dataService);

        IPObjectList comments = new PObjectList(mock(IDataService.class), List.of(comment));
        when(workItem.getComments()).thenReturn(comments);

        ListCleanerProvider.getInstance(IWorkItem.KEY_COMMENTS).clean(workItem);

        verify(dataService, times(1)).delete(comments);
    }

    @Test
    void testWorkflowSignaturesCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IWorkflowSignature workflowSignature = mock(IWorkflowSignature.class);
        IStatusOpt status = mock(IStatusOpt.class);
        when(workflowSignature.getTargetStatus()).thenReturn(status);
        when(workItem.getWorkflowSignatures()).thenReturn(List.of(workflowSignature));

        IWorkflowSignaturesManager workflowSignaturesManager = mock(IWorkflowSignaturesManager.class);
        when(workItem.getWorkflowSignaturesManager()).thenReturn(workflowSignaturesManager);

        ListCleanerProvider.getInstance(IWorkItem.KEY_WORKFLOW_SIGNATURES).clean(workItem);

        verify(workflowSignaturesManager, times(1)).removeWorkflowSignature(status);
    }

    @Test
    void testWorkRecordsCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IWorkRecord workRecord = mock(IWorkRecord.class);
        when(workItem.getWorkRecords()).thenReturn(new PObjectList(mock(IDataService.class), List.of(workRecord)));

        ListCleanerProvider.getInstance(IWorkItem.KEY_WORK_RECORDS).clean(workItem);

        verify(workItem, times(1)).deleteWorkRecord(workRecord);
    }

}

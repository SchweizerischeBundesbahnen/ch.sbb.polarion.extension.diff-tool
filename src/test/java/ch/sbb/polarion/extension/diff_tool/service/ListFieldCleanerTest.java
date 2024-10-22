package ch.sbb.polarion.extension.diff_tool.service;

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
public class ListFieldCleanerTest {

    @Test
    void testApprovalsCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IUser user = mock(IUser.class);
        IApprovalStruct approvalStruct = mock(IApprovalStruct.class);
        when(approvalStruct.getUser()).thenReturn(user);
        when(workItem.getApprovals()).thenReturn(List.of(approvalStruct));

        ListFieldCleaner.INSTANCES.get(IWorkItem.KEY_APPROVALS).accept(workItem);

        verify(workItem, times(1)).removeApprovee(user);
    }

    @Test
    void testAssigneeCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IUser user = mock(IUser.class);
        when(workItem.getAssignees()).thenReturn(new PObjectList(mock(IDataService.class), List.of(user)));

        ListFieldCleaner.INSTANCES.get(IWorkItem.KEY_ASSIGNEE).accept(workItem);

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

        ListFieldCleaner.INSTANCES.get(IWorkItem.KEY_COMMENTS).accept(workItem);

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

        ListFieldCleaner.INSTANCES.get(IWorkItem.KEY_WORKFLOW_SIGNATURES).accept(workItem);

        verify(workflowSignaturesManager, times(1)).removeWorkflowSignature(status);
    }

    @Test
    void testWorkRecordsCleanUp() {
        IWorkItem workItem = mock(IWorkItem.class);

        IWorkRecord workRecord = mock(IWorkRecord.class);
        when(workItem.getWorkRecords()).thenReturn(new PObjectList(mock(IDataService.class), List.of(workRecord)));

        ListFieldCleaner.INSTANCES.get(IWorkItem.KEY_WORK_RECORDS).accept(workItem);

        verify(workItem, times(1)).deleteWorkRecord(workRecord);
    }

}

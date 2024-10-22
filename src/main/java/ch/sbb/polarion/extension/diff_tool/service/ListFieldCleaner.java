package ch.sbb.polarion.extension.diff_tool.service;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.signatures.IWorkItemWorkflowSignature;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignature;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignaturesManager;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public abstract class ListFieldCleaner implements Consumer<IWorkItem> {
    public static final Map<String, ListFieldCleaner> INSTANCES = new TreeMap<>();

    static {
        INSTANCES.put(IWorkItem.KEY_APPROVALS, new ApprovalsCleaner());
        INSTANCES.put(IWorkItem.KEY_ASSIGNEE, new AssigneeCleaner());
        INSTANCES.put(IWorkItem.KEY_COMMENTS, new CommentsCleaner());
        INSTANCES.put(IWorkItem.KEY_WORKFLOW_SIGNATURES, new WorkflowSignaturesCleaner());
        INSTANCES.put(IWorkItem.KEY_WORK_RECORDS, new WorkRecordsCleaner());
    }

    private static class ApprovalsCleaner extends ListFieldCleaner {
        @Override
        @SuppressWarnings("unchecked")
        public void accept(IWorkItem workItem) {
            List<IUser> users = workItem.getApprovals().stream()
                    .filter(approval -> approval instanceof IApprovalStruct)
                    .map(approvalStruct -> ((IApprovalStruct) approvalStruct).getUser())
                    .toList();
            users.forEach(workItem::removeApprovee);
        }
    }

    private static class AssigneeCleaner extends ListFieldCleaner {
        @Override
        public void accept(IWorkItem workItem) {
            for (int i = 0; i < workItem.getAssignees().size(); i++) {
                Object assignee = workItem.getAssignees().get(i);
                if (assignee instanceof IUser user) {
                    workItem.removeAssignee(user);
                }
            }
        }
    }

    private static class CommentsCleaner extends ListFieldCleaner {
        @Override
        public void accept(IWorkItem workItem) {
            workItem.getDataSvc().delete(workItem.getComments());
        }
    }

    private static class WorkflowSignaturesCleaner extends ListFieldCleaner {
        @Override
        public void accept(IWorkItem workItem) {
            IWorkflowSignaturesManager<IWorkItemWorkflowSignature> workflowSignaturesManager = workItem.getWorkflowSignaturesManager();
            for (int i = 0; i < workItem.getWorkflowSignatures().size(); i++) {
                IWorkflowSignature workflowSignature = workItem.getWorkflowSignatures().get(i);
                if (workflowSignature.getTargetStatus() != null) {
                    workflowSignaturesManager.removeWorkflowSignature(workflowSignature.getTargetStatus());
                }
            }
        }
    }

    private static class WorkRecordsCleaner extends ListFieldCleaner {
        @Override
        public void accept(IWorkItem workItem) {
            for (int i = 0; i < workItem.getWorkRecords().size(); i++) {
                workItem.deleteWorkRecord(workItem.getWorkRecords().get(i));
            }
        }
    }
}

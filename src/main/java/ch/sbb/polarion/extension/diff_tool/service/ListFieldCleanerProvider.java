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

public final class ListFieldCleanerProvider {
    private static final Map<String, ListFieldCleaner> INSTANCES = new TreeMap<>();

    static {
        INSTANCES.put(IWorkItem.KEY_APPROVALS, new ApprovalsCleaner());
        INSTANCES.put(IWorkItem.KEY_ASSIGNEE, new AssigneeCleaner());
        INSTANCES.put(IWorkItem.KEY_COMMENTS, new CommentsCleaner());
        INSTANCES.put(IWorkItem.KEY_WORKFLOW_SIGNATURES, new WorkflowSignaturesCleaner());
        INSTANCES.put(IWorkItem.KEY_WORK_RECORDS, new WorkRecordsCleaner());
    }

    private ListFieldCleanerProvider() {
        // Factory class, not to be instantiated
    }

    public static ListFieldCleaner getInstance(String fieldName) {
        return INSTANCES.get(fieldName);
    }

    private static class ApprovalsCleaner implements ListFieldCleaner {
        @Override
        @SuppressWarnings("unchecked")
        public void clean(IWorkItem workItem) {
            List<IUser> users = workItem.getApprovals().stream()
                    .filter(approval -> approval instanceof IApprovalStruct)
                    .map(approvalStruct -> ((IApprovalStruct) approvalStruct).getUser())
                    .toList();
            users.forEach(workItem::removeApprovee);
        }
    }

    private static class AssigneeCleaner implements ListFieldCleaner {
        @Override
        public void clean(IWorkItem workItem) {
            for (int i = 0; i < workItem.getAssignees().size(); i++) {
                Object assignee = workItem.getAssignees().get(i);
                if (assignee instanceof IUser user) {
                    workItem.removeAssignee(user);
                }
            }
        }
    }

    private static class CommentsCleaner implements ListFieldCleaner {
        @Override
        public void clean(IWorkItem workItem) {
            workItem.getDataSvc().delete(workItem.getComments());
        }
    }

    private static class WorkflowSignaturesCleaner implements ListFieldCleaner {
        @Override
        public void clean(IWorkItem workItem) {
            IWorkflowSignaturesManager<IWorkItemWorkflowSignature> workflowSignaturesManager = workItem.getWorkflowSignaturesManager();
            for (int i = 0; i < workItem.getWorkflowSignatures().size(); i++) {
                IWorkflowSignature workflowSignature = workItem.getWorkflowSignatures().get(i);
                if (workflowSignature.getTargetStatus() != null) {
                    workflowSignaturesManager.removeWorkflowSignature(workflowSignature.getTargetStatus());
                }
            }
        }
    }

    private static class WorkRecordsCleaner implements ListFieldCleaner {
        @Override
        public void clean(IWorkItem workItem) {
            for (int i = 0; i < workItem.getWorkRecords().size(); i++) {
                workItem.deleteWorkRecord(workItem.getWorkRecords().get(i));
            }
        }
    }
}

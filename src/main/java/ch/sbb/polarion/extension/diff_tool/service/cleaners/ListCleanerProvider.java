package ch.sbb.polarion.extension.diff_tool.service.cleaners;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.model.signatures.IWorkItemWorkflowSignature;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignature;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignaturesManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ListCleanerProvider {
    private static final Map<String, ListCleaner> INSTANCES = new TreeMap<>();

    static {
        INSTANCES.put(IWorkItem.KEY_APPROVALS, new ApprovalsCleaner());
        INSTANCES.put(IWorkItem.KEY_ASSIGNEE, new AssigneeCleaner());
        INSTANCES.put(IWorkItem.KEY_COMMENTS, new CommentsCleaner());
        INSTANCES.put(IWorkflowObject.KEY_WORKFLOW_SIGNATURES, new WorkflowSignaturesCleaner());
        INSTANCES.put(IWorkItem.KEY_WORK_RECORDS, new WorkRecordsCleaner());
    }

    private ListCleanerProvider() {
        // Factory class, not to be instantiated
    }

    public static ListCleaner getInstance(String fieldName) {
        return INSTANCES.get(fieldName);
    }

    private static class ApprovalsCleaner implements ListCleaner {
        @Override
        @SuppressWarnings("unchecked")
        public void clean(@NotNull IWorkItem workItem) {
            List<IUser> users = workItem.getApprovals().stream()
                    .filter(IApprovalStruct.class::isInstance)
                    .map(approvalStruct -> ((IApprovalStruct) approvalStruct).getUser())
                    .toList();
            users.forEach(workItem::removeApprovee);
        }
    }

    private static class AssigneeCleaner implements ListCleaner {
        @Override
        public void clean(@NotNull IWorkItem workItem) {
            for (int i = 0; i < workItem.getAssignees().size(); i++) {
                Object assignee = workItem.getAssignees().get(i);
                if (assignee instanceof IUser user) {
                    workItem.removeAssignee(user);
                }
            }
        }
    }

    private static class CommentsCleaner implements ListCleaner {
        @Override
        public void clean(@NotNull IWorkItem workItem) {
            workItem.getDataSvc().delete(workItem.getComments());
        }
    }

    private static class WorkflowSignaturesCleaner implements ListCleaner {
        @Override
        public void clean(@NotNull IWorkItem workItem) {
            IWorkflowSignaturesManager<IWorkItemWorkflowSignature> workflowSignaturesManager = workItem.getWorkflowSignaturesManager();
            for (int i = 0; i < workItem.getWorkflowSignatures().size(); i++) {
                IWorkflowSignature workflowSignature = workItem.getWorkflowSignatures().get(i);
                if (workflowSignature.getTargetStatus() != null) {
                    workflowSignaturesManager.removeWorkflowSignature(workflowSignature.getTargetStatus());
                }
            }
        }
    }

    private static class WorkRecordsCleaner implements ListCleaner {
        @Override
        public void clean(@NotNull IWorkItem workItem) {
            for (int i = 0; i < workItem.getWorkRecords().size(); i++) {
                workItem.deleteWorkRecord(workItem.getWorkRecords().get(i));
            }
        }
    }
}

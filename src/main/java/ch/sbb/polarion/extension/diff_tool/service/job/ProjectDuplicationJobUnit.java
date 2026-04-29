package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.service.ProjectDuplicationService;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.IProgressMonitor;
import com.polarion.platform.jobs.spi.AbstractJobUnit;
import org.jetbrains.annotations.NotNull;

public class ProjectDuplicationJobUnit extends AbstractJobUnit {

    private final DuplicationRequest request;
    private final ProjectDuplicationService duplicationService;

    public ProjectDuplicationJobUnit(@NotNull DuplicationRequest request,
                                     @NotNull ProjectDuplicationService duplicationService,
                                     @NotNull IJobUnitFactory factory) {
        super(buildName(request), factory);
        this.request = request;
        this.duplicationService = duplicationService;
    }

    private static String buildName(@NotNull DuplicationRequest request) {
        return String.format("Duplicate project '%s' -> '%s'", request.getSourceProjectId(), request.getTargetProjectId());
    }

    @Override
    protected IJobStatus runInternal(IProgressMonitor monitor) {
        try {
            monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
            duplicationService.duplicate(request, getLogger());
            return getStatusOK("Project '" + request.getTargetProjectId() + "' has been created. Open: "
                    + ProjectDuplicationService.projectUrl(request.getTargetProjectId()));
        } catch (Exception e) {
            getLogger().error("Project duplication failed", e);
            return getStatusFailed("Project duplication failed: " + e.getMessage(), e);
        } finally {
            monitor.done();
        }
    }
}

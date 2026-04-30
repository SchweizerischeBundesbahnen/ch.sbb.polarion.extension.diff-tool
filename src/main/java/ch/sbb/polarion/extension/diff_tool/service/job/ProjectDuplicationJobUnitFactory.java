package ch.sbb.polarion.extension.diff_tool.service.job;

import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJobDescriptor;
import com.polarion.platform.jobs.IJobUnit;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.spi.BasicJobDescriptor;

/**
 * Minimal factory exposed only so {@link ProjectDuplicationJobUnit} can satisfy the
 * {@link IJobUnitFactory} contract required by {@code AbstractJobUnit}. The job unit is
 * always constructed in code with its runtime parameters, so {@link #createJobUnit(String)}
 * is not used and intentionally throws. The factory has no state.
 */
public final class ProjectDuplicationJobUnitFactory implements IJobUnitFactory {

    public static final String NAME = "diff-tool.project-duplication";
    public static final String LABEL = "Diff Tool: Project Duplication";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public IJobDescriptor getJobDescriptor(IJobUnit jobUnit) {
        return new BasicJobDescriptor(LABEL, jobUnit);
    }

    @Override
    public IJobUnit createJobUnit(String name) throws GenericJobException {
        throw new GenericJobException("ProjectDuplicationJobUnit must be constructed programmatically");
    }
}

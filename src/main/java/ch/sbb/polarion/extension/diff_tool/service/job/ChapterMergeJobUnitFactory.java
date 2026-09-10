package ch.sbb.polarion.extension.diff_tool.service.job;

import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJobDescriptor;
import com.polarion.platform.jobs.IJobUnit;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.spi.BasicJobDescriptor;

/**
 * Minimal factory exposed only so {@link ChapterMergeJobUnit} can satisfy the {@link IJobUnitFactory} contract
 * required by {@code AbstractJobUnit}, and so that chapter merge jobs can be told apart from other Polarion jobs.
 * The job unit is always constructed in code with its runtime parameters, so {@link #createJobUnit(String)}
 * is not used and intentionally throws. The factory has no state.
 */
public final class ChapterMergeJobUnitFactory implements IJobUnitFactory {

    public static final String NAME = "diff-tool.chapter-merge";
    public static final String LABEL = "Diff Tool: Chapter Merge";

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
        throw new GenericJobException("ChapterMergeJobUnit must be constructed programmatically");
    }
}

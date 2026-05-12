package ch.sbb.polarion.extension.diff_tool.service.job;

import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJobDescriptor;
import com.polarion.platform.jobs.IJobUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProjectDuplicationJobUnitFactoryTest {

    private final ProjectDuplicationJobUnitFactory factory = new ProjectDuplicationJobUnitFactory();

    @Test
    void getNameReturnsConstant() {
        assertEquals(ProjectDuplicationJobUnitFactory.NAME, factory.getName());
    }

    @Test
    void getJobDescriptorReturnsBasicDescriptorWithFactoryLabel() {
        IJobUnit unit = mock(IJobUnit.class);
        IJobDescriptor descriptor = factory.getJobDescriptor(unit);
        assertNotNull(descriptor);
        assertEquals(ProjectDuplicationJobUnitFactory.LABEL, descriptor.getLabel());
    }

    @Test
    void createJobUnitAlwaysThrows() {
        assertThrows(GenericJobException.class, () -> factory.createJobUnit("anything"));
    }
}

package ch.sbb.polarion.extension.diff_tool.rest.model.duplication;

import com.polarion.platform.jobs.IJobStatus.JobStatusType;
import com.polarion.platform.jobs.JobState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link JobStateName} and {@link JobStatusTypeName} stay in lock-step with
 * Polarion's pseudo-enums. The mirroring enums feed the generated OpenAPI schema; if Polarion
 * ever adds, removes or renames a state, this test fails loudly so the OpenAPI contract can be updated.
 */
class DuplicationJobInfoStatesTest {

    @Test
    void jobStateNamesMatchPolarionJobState() {
        assertEquals(polarionConstantNames(JobState.class, "STATE_", JobState.class, "getName"),
                Arrays.stream(JobStateName.values()).map(Enum::name).collect(Collectors.toSet()),
                "JobStateName drifted from com.polarion.platform.jobs.JobState — update the mirroring enum");
    }

    @Test
    void jobStatusTypeNamesMatchPolarionJobStatusType() {
        assertEquals(polarionConstantNames(JobStatusType.class, "STATUS_TYPE_", JobStatusType.class, "getName"),
                Arrays.stream(JobStatusTypeName.values()).map(Enum::name).collect(Collectors.toSet()),
                "JobStatusTypeName drifted from com.polarion.platform.jobs.IJobStatus.JobStatusType — update the mirroring enum");
    }

    private static Set<String> polarionConstantNames(Class<?> ownerClass,
                                                     String fieldPrefix,
                                                     Class<?> expectedFieldType,
                                                     String nameAccessor) {
        return Arrays.stream(ownerClass.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers())
                        && f.getName().startsWith(fieldPrefix)
                        && expectedFieldType.isAssignableFrom(f.getType()))
                .map(f -> invokeNameAccessor(f, nameAccessor))
                .collect(Collectors.toSet());
    }

    private static String invokeNameAccessor(Field field, String nameAccessor) {
        try {
            Object instance = field.get(null);
            return (String) instance.getClass().getMethod(nameAccessor).invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read Polarion constant " + field.getName(), e);
        }
    }
}

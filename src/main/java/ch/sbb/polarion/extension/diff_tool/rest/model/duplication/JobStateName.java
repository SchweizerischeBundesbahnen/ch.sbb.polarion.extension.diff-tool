package ch.sbb.polarion.extension.diff_tool.rest.model.duplication;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Mirrors the names returned by {@code com.polarion.platform.jobs.JobState.getName()}.
 * Polarion's {@code JobState} is a pseudo-enum (final class with public static fields),
 * so Swagger cannot derive the value list from it directly — this enum gives swagger-maven-plugin
 * a typed reference, producing an {@code enum: [...]} constraint in the generated OpenAPI schema.
 * Kept in sync with Polarion by {@code DuplicationJobInfoStatesTest}.
 */
@Schema(description = "Polarion job state")
public enum JobStateName {
    UNSCHEDULED,
    ACTIVATING,
    WAITING,
    RUNNING,
    FINISHED,
    ABORTED
}

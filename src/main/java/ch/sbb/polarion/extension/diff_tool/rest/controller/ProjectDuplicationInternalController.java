package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.ProjectInfo;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.job.ProjectDuplicationJobScheduler;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Comparator;
import java.util.List;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Project Duplication")
public class ProjectDuplicationInternalController {

    protected final PolarionService polarionService;
    protected final ProjectDuplicationJobScheduler scheduler;

    public ProjectDuplicationInternalController() {
        this(new PolarionService(), new ProjectDuplicationJobScheduler());
    }

    public ProjectDuplicationInternalController(PolarionService polarionService, ProjectDuplicationJobScheduler scheduler) {
        this.polarionService = polarionService;
        this.scheduler = scheduler;
    }

    @GET
    @Path("/projects")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Lists all projects readable by the current user (used to populate the source-project dropdown)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of projects",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ProjectInfo.class)
                            )
                    )
            }
    )
    public List<ProjectInfo> listProjects() {
        return polarionService.getProjects().stream()
                .map(p -> ProjectInfo.builder().id(p.getId()).name(p.getName()).build())
                .sorted(Comparator.comparing(p -> p.getName() == null ? p.getId() : p.getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @POST
    @Path("/projects/duplicate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Schedules an asynchronous job that duplicates a project via template export/import",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Job scheduled; response carries the Polarion job id and a URL to the job monitor",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DuplicationJobInfo.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "403", description = "Not authorized")
            }
    )
    public DuplicationJobInfo duplicateProject(DuplicationRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (!polarionService.hasSufficientPermissions()) {
            throw new ForbiddenException("Project duplication is not allowed");
        }
        try {
            return scheduler.schedule(request);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/projects/duplicate/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Lists all project duplication jobs created by this extension, newest first",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of jobs",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DuplicationJobInfo.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Not authorized")
            }
    )
    public List<DuplicationJobInfo> listDuplicationJobs() {
        if (!polarionService.hasSufficientPermissions()) {
            throw new ForbiddenException("Project duplication is not allowed");
        }
        return scheduler.listJobs();
    }
}

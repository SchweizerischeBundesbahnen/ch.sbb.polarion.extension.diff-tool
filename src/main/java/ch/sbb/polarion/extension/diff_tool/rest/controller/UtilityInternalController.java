package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Document;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentRevision;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Space;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.model.IStatusOpt;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

@Hidden
@Path("/internal")
@Tag(name = "Utility API")
public class UtilityInternalController {
    private static final String MISSING_PROJECT_ID_MESSAGE = "'projectId' should be provided";

    protected final PolarionService polarionService = new PolarionService();

    @GET
    @Path("/projects/{projectId}/spaces")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of spaces (folders) located in specified project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of spaces",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = Space.class)
                            )
                    )
            })
    public Collection<Space> getSpaces(@PathParam("projectId") String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new BadRequestException(MISSING_PROJECT_ID_MESSAGE);
        }
        return polarionService.getSpaces(projectId).stream().map(space -> new Space(space.getName())).toList();
    }

    @GET
    @Path("/projects/{projectId}/spaces/{spaceId}/documents")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of documents located in specified space of specified project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of documents",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = Document.class)
                            )
                    )
            }
    )
    public List<Document> getDocuments(@PathParam("projectId") String projectId, @PathParam("spaceId") String spaceId) {
        if (StringUtils.isBlank(projectId) || StringUtils.isBlank(spaceId)) {
            throw new BadRequestException("'projectId' and 'spaceId' should be provided");
        }
        IProject project = polarionService.getProject(projectId);
        return polarionService.getDocuments(projectId, spaceId).stream()
                .map(document -> Document.builder().projectName(project.getName()).spaceId(document.getModuleFolder()).id(document.getId()).title(document.getTitleOrName()).build())
                .toList();
    }

    @GET
    @Path("/projects/{projectId}/spaces/{spaceId}/documents/{docName}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of revisions for the document located in specified space of specified project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of revisions for the specified document",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DocumentRevision.class)
                            )
                    )
            }
    )
    public List<DocumentRevision> getDocumentRevisions(@PathParam("projectId") String projectId, @PathParam("spaceId") String spaceId, @PathParam("docName") String docName) {
        if (StringUtils.isBlank(projectId) || StringUtils.isBlank(spaceId) || StringUtils.isBlank(docName)) {
            throw new BadRequestException("'projectId', 'spaceId' and 'docName' should be provided");
        }
        return polarionService.getDocumentRevisions(polarionService.getModule(projectId, spaceId, docName));
    }

    @GET
    @Path("/projects/{projectId}/workitem-fields")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets full list of all general and custom fields configured for all kind of work items in specified project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of all work item fields for the specified project",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = WorkItemField.class)
                            )
                    )
            }
    )
    public List<WorkItemField> getAllWorkItemFields(@PathParam("projectId") String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new BadRequestException(MISSING_PROJECT_ID_MESSAGE);
        }
        return polarionService.getAllWorkItemFields(projectId);
    }

    @GET
    @Path("/projects/{projectId}/workitem-statuses")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of all statuses configured for all kind of work items in specified project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of all work item statuses for the specified project",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = IStatusOpt.class)
                            )
                    )
            }
    )
    public Collection<IStatusOpt> getAllWorkItemStatuses(@PathParam("projectId") String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new BadRequestException(MISSING_PROJECT_ID_MESSAGE);
        }
        return polarionService.getWorkItemStatuses(projectId);
    }
}

package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.properties.DiffToolExtensionConfiguration;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentDuplicateParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.CommunicationSettings;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Document;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentRevision;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Space;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemStatus;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.LinkRole;
import ch.sbb.polarion.extension.diff_tool.service.DocumentCopyService;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.generic.util.ExtensionInfo;
import com.polarion.alm.projects.model.IProject;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

@Hidden
@Path("/internal")
@Tag(name = "Utility API")
public class UtilityInternalController {
    private static final String MISSING_PROJECT_ID_MESSAGE = "'projectId' should be provided";

    protected final PolarionService polarionService = new PolarionService();
    private final DocumentCopyService duplicateService = new DocumentCopyService(polarionService, new MergeService(polarionService));

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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/{projectId}/spaces/{spaceId}/documents/{documentName}/duplicate")
    @Operation(summary = "Creates a new document as a duplicate of specified one",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = DocumentDuplicateParams.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Document created successfully"),
                    @ApiResponse(responseCode = "409", description = "Target document already exists")
            }
    )
    public DocumentIdentifier createDocumentDuplicate(@Parameter(description = "Project ID of source document") @PathParam("projectId") String sourceProjectId,
                                                      @Parameter(description = "Space ID of source document") @PathParam("spaceId") String sourceSpaceId,
                                                      @Parameter(description = "Source document name") @PathParam("documentName") String sourceDocumentName,
                                                      @Parameter(description = "Optional revision of source document") @QueryParam("revision") String revision,
                                                      DocumentDuplicateParams documentDuplicateParams) {
        if (StringUtils.isBlank(sourceProjectId) || StringUtils.isBlank(sourceSpaceId) || StringUtils.isBlank(sourceDocumentName) || documentDuplicateParams == null) {
            throw new BadRequestException("'projectId', 'spaceId', 'documentName' and request body should be provided");
        }
        return duplicateService.createDocumentDuplicate(sourceProjectId, sourceSpaceId, sourceDocumentName, revision, documentDuplicateParams);
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
                                    schema = @Schema(implementation = WorkItemStatus.class)
                            )
                    )
            }
    )
    public Collection<WorkItemStatus> getAllWorkItemStatuses(@PathParam("projectId") String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new BadRequestException(MISSING_PROJECT_ID_MESSAGE);
        }
        return polarionService.getWorkItemStatuses(projectId);
    }

    @GET
    @Path("/projects/{projectId}/hyperlink-roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of all hyperlink roles in the specified project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of all hyperlink roles for the specified project",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = LinkRole.class)
                            )
                    )
            }
    )
    public Collection<LinkRole> getAllHyperlinkRoles(@PathParam("projectId") String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new BadRequestException(MISSING_PROJECT_ID_MESSAGE);
        }
        return polarionService.getHyperlinkRoles(projectId);
    }

    @GET
    @Path("/projects/{projectId}/linked-workitem-roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of all linked WorkItem roles in the specified project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of all linked WorkItem roles for the specified project",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = LinkRole.class)
                            )
                    )
            }
    )
    public Collection<LinkRole> getAllLinkedWorkItemRoles(@PathParam("projectId") String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new BadRequestException(MISSING_PROJECT_ID_MESSAGE);
        }
        return polarionService.getLinkedWorkItemRoles(projectId);
    }

    @GET
    @Path("/communication/settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets communication settings for JS client",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Communication settings in JSON format",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = CommunicationSettings.class)
                            )
                    )
            }
    )
    public CommunicationSettings getCommunicationSettings() {
        Integer chunkSize = DiffToolExtensionConfiguration.getInstance().getChunkSize();
        return CommunicationSettings.builder()
                .chunkSize(chunkSize)
                .build();
    }

    @GET
    @Path("/extension/info")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets extension information",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Extension information in JSON format",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ExtensionInfo.class)
                            )
                    )
            }
    )
    public ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.getInstance();
    }
}

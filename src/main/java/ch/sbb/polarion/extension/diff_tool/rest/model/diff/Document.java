package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.model.IModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import static ch.sbb.polarion.extension.diff_tool.rest.model.diff.Space.DEFAULT_SPACE_ID;
import static ch.sbb.polarion.extension.diff_tool.rest.model.diff.Space.DEFAULT_SPACE_NAME;

@Builder
@Getter
@Setter
@Schema(description = "Represents a document within a project, including metadata such as projectID, space ID, and revision details")
public class Document {
    @Schema(description = "The ID of the project this document belongs to")
    private String projectId;

    @Schema(description = "The name of the project this document belongs to")
    private String projectName;

    @Schema(description = "The ID of the space where the document is located")
    private String spaceId;

    @Schema(description = "The location path of the document")
    private String locationPath;

    @Schema(description = "The unique identifier of the document")
    private String id;

    @Schema(description = "The title of the document")
    private String title;

    @Schema(description = "The current revision of the document")
    private String revision;

    @Schema(description = "The XML revision of the module")
    private String moduleXmlRevision;

    @Schema(description = "The head revision of the document")
    private String headRevision;

    @Schema(description = "Indicates whether the user is authorized for merge operations")
    private boolean authorizedForMerge;

    @JsonGetter
    public String getSpaceName() {
        return DEFAULT_SPACE_ID.equals(spaceId) ? DEFAULT_SPACE_NAME : spaceId;
    }

    public Document authorizedForMerge(boolean authorizedForMerge) {
        this.authorizedForMerge = authorizedForMerge;
        return this;
    }

    public static Document from(@Nullable IModule module) {
        if (module == null) {
            return null;
        }

        IProject project = module.getProject();
        return Document.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .spaceId(module.getModuleFolder())
                .locationPath(module.getModuleLocation().getLocationPath())
                .id(module.getId())
                .title(module.getTitleOrName())
                .revision(module.getRevision())
                .moduleXmlRevision(module.getLastRevision())
                .headRevision(module.getDataSvc().getLastStorageRevision().getName())
                .build();
    }
}

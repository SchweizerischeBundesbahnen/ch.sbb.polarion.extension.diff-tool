package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Documents merge input data")
public class DocumentsMergeParams extends MergeParams {

    @Schema(description = "Identifier for the left document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier leftDocument;

    @Schema(description = "Identifier for the right document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier rightDocument;

    @Schema(description = "Indicates if merging referenced Work Item allowed")
    private Boolean allowReferencedWorkItemMerge;

    @Schema(description = "Indicates whether comments must be preserved during merge operation")
    private boolean preserveComments;

    @SuppressWarnings("java:S107")
    public DocumentsMergeParams(DocumentIdentifier leftDocument, DocumentIdentifier rightDocument, MergeDirection direction, String linkRole,
                                String configName, String configCacheBucketId, List<WorkItemsPair> pairs, Boolean allowReferencedWorkItemMerge) {
        super(direction, linkRole, configName, configCacheBucketId, pairs);
        this.leftDocument = leftDocument;
        this.rightDocument = rightDocument;
        this.allowReferencedWorkItemMerge = allowReferencedWorkItemMerge;
    }

    @JsonIgnore
    public boolean isAllowedReferencedWorkItemMerge() {
        return Boolean.TRUE.equals(allowReferencedWorkItemMerge);
    }
}

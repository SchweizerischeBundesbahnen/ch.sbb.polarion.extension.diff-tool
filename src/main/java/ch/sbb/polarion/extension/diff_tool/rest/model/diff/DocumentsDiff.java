package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
@Schema(description = "Represents the difference between two documents")
public class DocumentsDiff {

    @Schema(description = "The left document in the comparison", implementation = Document.class)
    private Document leftDocument;

    @Schema(description = "The right document in the comparison", implementation = Document.class)
    private Document rightDocument;

    @Schema(description = "A collection of paired WorkItems between the two documents", implementation = WorkItemsPair.class)
    private Collection<WorkItemsPair> pairedWorkItems;
}

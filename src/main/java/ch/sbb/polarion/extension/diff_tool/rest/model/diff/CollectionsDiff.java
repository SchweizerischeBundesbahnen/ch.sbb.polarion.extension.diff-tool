package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
@Schema(description = "Represents the difference between two collections")
public class CollectionsDiff {
    @Schema(description = "Left collection in the comparison", implementation = DocumentsCollection.class)
    private DocumentsCollection leftCollection;

    @Schema(description = "Right collection in the comparison", implementation = DocumentsCollection.class)
    private DocumentsCollection rightCollection;

    @Schema(description = "A collection of paired documents", implementation = DocumentsPair.class)
    private Collection<DocumentsPair> pairedDocuments;

}

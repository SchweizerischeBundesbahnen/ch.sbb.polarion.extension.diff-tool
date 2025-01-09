package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Collections comparison input data")
public class CollectionsDiffParams {
    @Schema(description = "Identification data of left collection in the comparison", implementation = DocumentsCollection.class)
    private DocumentsCollection leftCollection;

    @Schema(description = "Identification data of right collection in the comparison", implementation = DocumentsCollection.class)
    private DocumentsCollection rightCollection;

}

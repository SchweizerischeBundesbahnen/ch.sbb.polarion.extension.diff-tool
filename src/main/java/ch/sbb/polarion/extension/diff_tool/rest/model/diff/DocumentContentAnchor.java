package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents an anchor to which document content blocks are tied")
public class DocumentContentAnchor {

    public enum ContentPosition {
        ABOVE("above"), BELOW("below");

        private final String name;

        ContentPosition(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Schema(description = "ID of the WorkItem which represents the anchor")
    private String id;

    @Schema(description = "Title of the WorkItem")
    private String title;

    @Schema(description = "Outline number of the WorkItem")
    private String outlineNumber;

    @Schema(description = "Content block above the anchor")
    private String contentAbove;

    @Schema(description = "Content block below the anchor")
    private String contentBelow;

    @Schema(description = "Diff of content block above the anchor")
    private String diffAbove;

    @Schema(description = "Diff of content block below the anchor")
    private String diffBelow;
}

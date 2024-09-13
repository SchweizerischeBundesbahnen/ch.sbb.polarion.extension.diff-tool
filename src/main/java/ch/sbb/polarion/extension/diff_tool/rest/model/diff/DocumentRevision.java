package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.core.util.logging.Logger;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Revision details")
public class DocumentRevision implements Comparable<DocumentRevision> {

    private static final Logger logger = Logger.getLogger(DocumentRevision.class);

    @Schema(description = "The name of the revision")
    private String name;

    @Schema(description = "The baseline of the revision")
    private String baselineName;

    public int compareTo(@NotNull DocumentRevision that) {
        long thisRevision = 0L;
        long thatRevision = 0L;

        try {
            thisRevision = Long.parseLong(this.name);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected revision name found: " + this.name);
        }

        try {
            thatRevision = Long.parseLong(that.name);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected revision name found: " + this.name);
        }

        return Long.compare(thatRevision, thisRevision); // Reverse sorting is used to show most last revision as first
    }
}

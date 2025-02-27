package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor.ContentPosition.ABOVE;
import static ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor.ContentPosition.BELOW;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a pair of documents content anchors for diffing/merging content blocks which potentially linked to them")
public class DocumentContentAnchorsPair {

    @Schema(description = "The left content anchor in the pair", implementation = DocumentContentAnchor.class)
    private DocumentContentAnchor leftAnchor;

    @Schema(description = "The right content anchor in the pair", implementation = DocumentContentAnchor.class)
    private DocumentContentAnchor rightAnchor;

    public String getLeftContent(@NotNull DocumentContentAnchor.ContentPosition contentPosition) {
        return switch (contentPosition) {
            case ABOVE -> leftAnchor == null ? "" : leftAnchor.getContentAbove();
            case BELOW -> leftAnchor == null ? "" : leftAnchor.getContentBelow();
        };
    }

    public String getRightContent(@NotNull DocumentContentAnchor.ContentPosition contentPosition) {
        return switch (contentPosition) {
            case ABOVE -> rightAnchor == null ? "" : rightAnchor.getContentAbove();
            case BELOW -> rightAnchor == null ? "" : rightAnchor.getContentBelow();
        };
    }

    public void setLeftDiff(String diff, @NotNull DocumentContentAnchor.ContentPosition contentPosition) {
        if (leftAnchor != null) {
            setDiff(diff, contentPosition, leftAnchor);
        }
    }

    public void setRightDiff(String diff, @NotNull DocumentContentAnchor.ContentPosition contentPosition) {
        if (rightAnchor != null) {
            setDiff(diff, contentPosition, rightAnchor);
        }
    }

    private void setDiff(String diff, @NotNull DocumentContentAnchor.ContentPosition contentPosition, @NotNull DocumentContentAnchor anchor) {
        if (contentPosition == ABOVE) {
            anchor.setDiffAbove(diff);
        } else if (contentPosition == BELOW) {
            anchor.setDiffBelow(diff);
        }
    }
}

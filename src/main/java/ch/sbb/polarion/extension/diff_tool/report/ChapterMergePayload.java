package ch.sbb.polarion.extension.diff_tool.report;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifies a chapter merge operation as a whole, for report entries which describe neither a work item pair
 * nor a field, eg. the chapter which was merged or a work item layout copied into the target document.
 */
@Schema(description = "Chapter merge operation data")
public record ChapterMergePayload(
        @Schema(description = "Document content is taken from, as 'projectId/spaceId/name'") @NotNull String sourceDocument,
        @Schema(description = "Document content is placed into, as 'projectId/spaceId/name'") @NotNull String targetDocument,
        @Schema(description = "Outline number of the merged chapter in the source document") @Nullable String sourceOutlineNumber,
        @Schema(description = "Outline number of the anchor chapter in the target document") @Nullable String targetOutlineNumber,
        @Schema(description = "Outline number the merged chapter got in the target document") @Nullable String insertedOutlineNumber) {
}

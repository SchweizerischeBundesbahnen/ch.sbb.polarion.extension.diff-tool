package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Declares whether work items of a merged chapter are copied or moved")
public enum ChapterMergeMode {

    @Schema(description = "New work items are created in the target document")
    COPY("Copy", "New work items are created in the target document. Source work items stay untouched."),

    @Schema(description = "Work items are moved out of the source document. Headings are copied in any case")
    MOVE("Move", "Work items are moved into the target document and removed from the source one. Headings are copied, not moved.");

    private final String title;
    private final String description;

    ChapterMergeMode(String title, String description) {
        this.title = title;
        this.description = description;
    }
}

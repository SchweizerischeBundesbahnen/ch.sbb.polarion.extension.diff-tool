package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Declares where merged content is placed relatively to the target chapter")
public enum ChapterInsertMode {

    @Schema(description = "Content is placed directly under the target chapter, shifting its existing content below")
    UNDER("Under", "Content is placed directly under the target chapter, shifting existing content below."),

    @Schema(description = "Content becomes a new chapter of the same level as the target chapter, placed right after it")
    AFTER("After", "Content becomes a new chapter of the same level as the target chapter, placed right after it.");

    private final String title;
    private final String description;

    ChapterInsertMode(String title, String description) {
        this.title = title;
        this.description = description;
    }
}

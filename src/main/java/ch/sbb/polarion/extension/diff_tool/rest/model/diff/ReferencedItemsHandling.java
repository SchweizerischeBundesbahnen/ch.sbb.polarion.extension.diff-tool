package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Declares how referenced (external) work items of a merged chapter should be handled")
public enum ReferencedItemsHandling {

    @Schema(description = "The same work item is referenced in the target document")
    KEEP_REFERENCE("Keep reference", "The same work item is referenced in the target document."),

    @Schema(description = "A new work item is created in the target project out of the referenced one")
    COPY_AS_NEW("Copy as new", "A new work item is created in the target project out of the referenced one."),

    @Schema(description = "Referenced work items are skipped and reported")
    SKIP("Skip", "Referenced work items are not merged, a warning is reported for each of them.");

    private final String title;
    private final String description;

    ReferencedItemsHandling(String title, String description) {
        this.title = title;
        this.description = description;
    }
}

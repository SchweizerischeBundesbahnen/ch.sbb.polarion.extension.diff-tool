package ch.sbb.polarion.extension.diff_tool.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Declares how references should be handled during copy operation")
public enum HandleReferencesType {

    @Schema(description = "Default implementation (references will be pointed to the counterparts or removed if counterpart is not found)")
    DEFAULT(
            "Remove when no counterpart found",
            "Attempts to replace reference with its counterpart (workitem which is linked using specified role). If no counterpart found, reference will be removed."
    ),

    @Schema(description = "If no counterparts found references will be pointed to the newly created workitems")
    CREATE_MISSING(
            "Create missing counterpart",
            "Attempts to replace reference with its counterpart (workitem which is linked using specified role). If no counterpart found, reference will be replaced with a newly created workitem."
    ),

    @Schema(description = "If no counterparts found original references will be kept")
    KEEP(
            "Keep when no counterpart found",
            "Attempts to replace reference with its counterpart (workitem which is linked using specified role). If no counterpart found, original reference will be kept."
    ),

    @Schema(description = "All references will be overwritten with the newly created workitems")
    ALWAYS_OVERWRITE(
            "Always overwrite with newly created",
            "All references will be replaced with newly created workitems. No attempts to find counterparts will be made."
    );

    private final String title;
    private final String description;

    HandleReferencesType(String title, String description) {
        this.title = title;
        this.description = description;
    }

}

package ch.sbb.polarion.extension.diff_tool.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Declares how references should be handled during copy operation")
public enum HandleReferencesType {

    @Schema(description = "Default implementation (references will be pointed to the counterparts or removed if counterpart is not found)")
    DEFAULT,

    @Schema(description = "All references will be overwritten with the newly created workitems")
    ALWAYS_OVERWRITE
}

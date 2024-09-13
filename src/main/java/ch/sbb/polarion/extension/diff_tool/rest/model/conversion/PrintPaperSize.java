package ch.sbb.polarion.extension.diff_tool.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Schema(description = "Enum representing print paper sizes")
public enum PrintPaperSize {
    @Schema(description = "A4 paper size")
    A4,

    @Schema(description = "A3 paper size")
    A3;

    @JsonCreator
    public static PrintPaperSize fromString(String name) {
        try {
            return (name != null) ? valueOf(name.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            // Necessary to return correct HTTP error code by query parameters conversion
            throw new WebApplicationException(javax.ws.rs.core.Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Unsupported value for paperSize parameter: " + name)
                    .build());
        }
    }
}

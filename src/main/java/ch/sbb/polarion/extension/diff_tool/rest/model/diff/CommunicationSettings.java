package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Communication settings for JS client")
public class CommunicationSettings {
    @Schema(description = "The number of requests to the server that are allowed to run in parallel")
    private Integer chunkSize;

    public Integer getChunkSize() {
        return Math.max(1, chunkSize);
    }
}

package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@EqualsAndHashCode
@Schema(description = "Represents a space within a project")
public class Space implements Comparable<Space> {
    public static final String DEFAULT_SPACE_ID = "_default";
    public static final String DEFAULT_SPACE_NAME = "Default Space";

    @Schema(description = "Space ID")
    private final String id;

    public Space(@NotNull String id) {
        this.id = id;
    }

    @Schema(description = "The name of the space")
    public String getName() {
        return DEFAULT_SPACE_ID.equals(id) ? DEFAULT_SPACE_NAME : id;
    }

    @Override
    public int compareTo(@NotNull Space o) {
        return id.compareTo(o.id);
    }
}

package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.projects.model.IFolder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Getter
@EqualsAndHashCode
@Schema(description = "Represents a space within a project")
public class Space implements Comparable<Space> {
    public static final String DEFAULT_SPACE_ID = "_default";
    public static final String DEFAULT_SPACE_NAME = "Default Space";

    @Schema(description = "Space ID")
    private final String id;

    @Schema(description = "The title of the space, its ID when it has none")
    private final String title;

    public Space(@NotNull String id) {
        this(id, id);
    }

    public Space(@NotNull String id, @NotNull String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * The space of the given folder. A folder without a title gets its ID as the title.
     */
    public static @NotNull Space from(@NotNull IFolder folder) {
        String title = folder.getTitleOrName();
        return new Space(folder.getName(), StringUtils.isBlank(title) ? folder.getName() : title);
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

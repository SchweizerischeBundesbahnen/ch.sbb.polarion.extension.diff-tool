package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.service.CalculatePairsContext;
import com.polarion.alm.tracker.model.IWorkItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a pair of WorkItems for comparison")
public class WorkItemsPair {

    @Schema(description = "The left WorkItem in the pair", implementation = WorkItem.class)
    private WorkItem leftWorkItem;

    @Schema(description = "The right WorkItem in the pair", implementation = WorkItem.class)
    private WorkItem rightWorkItem;

    public static WorkItemsPair of(@NotNull IWorkItem leftWorkItem, @Nullable IWorkItem rightWorkItem) {
        return WorkItemsPair.builder().leftWorkItem(WorkItem.of(leftWorkItem)).rightWorkItem(WorkItem.of(rightWorkItem)).build();
    }

    public static WorkItemsPair of(@NotNull Pair<IWorkItem, IWorkItem> pair, @NotNull CalculatePairsContext context) {
        WorkItem left = WorkItem.of(
                pair.getLeft(),
                context.getOutlineNumber(pair.getLeft(), true),
                context.isReferenced(pair.getLeft(), true),
                pair.getLeft() != null && !context.getLeftDocument().getProjectId().equals(pair.getLeft().getProjectId())
        );
        WorkItem right = WorkItem.of(
                pair.getRight(),
                context.getOutlineNumber(pair.getRight(), false),
                context.isReferenced(pair.getRight(), false),
                pair.getRight() != null && !context.getRightDocument().getProjectId().equals(pair.getRight().getProjectId())
        );
        return WorkItemsPair.builder().leftWorkItem(left).rightWorkItem(right).build();
    }

}

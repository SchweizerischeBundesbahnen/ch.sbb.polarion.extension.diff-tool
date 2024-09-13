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

    public static WorkItemsPair of(@NotNull Pair<IWorkItem, IWorkItem> pair, @NotNull CalculatePairsContext context) {
        WorkItem left = WorkItem.of(
                pair.getLeft(),
                context.getOutlineNumber(pair.getLeft(), true),
                context.isReferenced(pair.getLeft(), true)
        );
        WorkItem right = WorkItem.of(
                pair.getRight(),
                context.getOutlineNumber(pair.getRight(), false),
                context.isReferenced(pair.getRight(), false)
        );
        return WorkItemsPair.builder().leftWorkItem(left).rightWorkItem(right).build();
    }

}

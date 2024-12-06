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
@Schema(description = "Represents a pair of documents fields for comparison")
public class DocumentsFieldsPair {

    @Schema(description = "The left field in the pair", implementation = WorkItem.Field.class)
    private WorkItem.Field leftField;

    @Schema(description = "The right field in the pair", implementation = WorkItem.Field.class)
    private WorkItem.Field rightField;
}

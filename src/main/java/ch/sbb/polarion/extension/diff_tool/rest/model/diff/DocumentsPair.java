package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.tracker.model.IModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a pair of Documents for comparison")
public class DocumentsPair {
    @Schema(description = "The left document in the pair", implementation = Document.class)
    private Document leftDocument;

    @Schema(description = "The right document in the pair", implementation = Document.class)
    private Document rightDocument;

    public static DocumentsPair of(@NotNull IModule leftDocument, @Nullable IModule rightDocument) {
        return DocumentsPair.builder().leftDocument(Document.from(leftDocument)).rightDocument(Document.from(rightDocument)).build();
    }

}

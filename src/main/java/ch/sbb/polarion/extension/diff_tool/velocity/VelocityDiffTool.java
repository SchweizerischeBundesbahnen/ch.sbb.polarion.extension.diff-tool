package ch.sbb.polarion.extension.diff_tool.velocity;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.StringsDiff;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class VelocityDiffTool {

    @NotNull
    public StringsDiff diffText(@Nullable String text1, @Nullable String text2) {
        return DiffToolUtils.diffText(text1, text2);
    }

    @NotNull
    public StringsDiff diffHtml(@Nullable String html1, @Nullable String html2) {
        return DiffToolUtils.diffHtml(html1, html2);
    }
}

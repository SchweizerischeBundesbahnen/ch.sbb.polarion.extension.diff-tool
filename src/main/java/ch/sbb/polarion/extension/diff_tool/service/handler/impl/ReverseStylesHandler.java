package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import org.jetbrains.annotations.NotNull;

import static ch.sbb.polarion.extension.diff_tool.service.ModifiedHtmlSaxDiffOutput.CLASS_HTML_ADDED;
import static ch.sbb.polarion.extension.diff_tool.service.ModifiedHtmlSaxDiffOutput.CLASS_HTML_REMOVED;

/**
 * Swaps added and removed styles to properly show reversed diff results.
 */
public class ReverseStylesHandler implements DiffLifecycleHandler {

    private static final String TEMP_CLASS = "temp_class_name_to_replace";

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        return !context.reverseStyles() ? diff : diff
                .replace(CLASS_HTML_REMOVED, TEMP_CLASS)
                .replace(CLASS_HTML_ADDED, CLASS_HTML_REMOVED)
                .replace(TEMP_CLASS, CLASS_HTML_ADDED);
    }
}

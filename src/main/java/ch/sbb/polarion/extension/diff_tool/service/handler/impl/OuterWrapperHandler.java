package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * It appears that DaisyDiff has the following bug: when we try to diff basic strings with the tags inside - last text part of diff will be missing.
 * For example if we try to compare 'some example text' and 'some <span>example</span> text' the last word 'text' will be missing in the resulting diff.
 * Also looks like in case if the string values wrapped in any tag the issue disappears.
 * So this handler basically wraps initial pair entries with the fake tags and removes them afterwards.
 */
public class OuterWrapperHandler implements DiffLifecycleHandler {

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        return Pair.of(DiffToolUtils.wrapInSurrogateTag(initialPair.getLeft()), DiffToolUtils.wrapInSurrogateTag(initialPair.getRight()));
    }

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        return DiffToolUtils.unwrapSurrogateTag(diff);
    }
}

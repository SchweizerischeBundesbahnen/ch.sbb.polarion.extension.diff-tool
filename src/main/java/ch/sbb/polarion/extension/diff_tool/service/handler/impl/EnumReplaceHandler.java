package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import com.polarion.subterra.base.data.model.IEnumType;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Workaround to make enums diff more convenient (show as a whole string change).
 * Runs diff using fake values and replace them with the enum htmls afterwards.
 */
public class EnumReplaceHandler implements DiffLifecycleHandler {

    private static final String REPLACE_TOKEN_LEFT = "LeftTokenToReplace";
    private static final String REPLACE_TOKEN_RIGHT = "RightTokenToReplace";

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        return isEnumDiff(context.fieldA, context.fieldB) ? Pair.of(REPLACE_TOKEN_LEFT, REPLACE_TOKEN_RIGHT) : initialPair;
    }

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        return isEnumDiff(context.fieldA, context.fieldB) ?
                diff.replace(REPLACE_TOKEN_LEFT, context.fieldA.getHtml()).replace(REPLACE_TOKEN_RIGHT, context.fieldB.getHtml()) : diff;
    }

    private boolean isEnumDiff(WorkItem.Field fieldA, WorkItem.Field fieldB) {
        return fieldA.getType() instanceof IEnumType || fieldB.getType() instanceof IEnumType;
    }
}

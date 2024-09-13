package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class IdsHandler implements DiffLifecycleHandler {

    private static final String ID = "id";

    private final Map<String, String> placeholdersMapping = new HashMap<>();

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        if (applicable(context) && context.workItemA != null && context.workItemB != null) {
            placeholdersMapping.put(initialPair.getLeft(), initialPair.getRight());
            return Pair.of(initialPair.getLeft(), initialPair.getLeft());
        }
        return initialPair;
    }

    @NotNull
    @Override
    public String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        if (applicable(context) && placeholdersMapping.containsKey(diff)) {
            return placeholdersMapping.get(diff);
        } else {
            return diff;
        }
    }

    private boolean applicable(@NotNull DiffContext context) {
        String fieldId = Optional.ofNullable(context.fieldA.getId()).orElse(context.fieldB.getId());
        return ID.equals(fieldId) && !context.pairedWorkItemsDiffer;
    }
}

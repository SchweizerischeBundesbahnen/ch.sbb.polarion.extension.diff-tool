package ch.sbb.polarion.extension.diff_tool.service;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.platform.persistence.model.IStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Copies the work item configuration a document itself holds: the rendering layout of a work item type
 * (which fields it shows and how) and the list of types the document allows.
 * <p>
 * Only the document is modified, never the project configuration. A layout which already exists in the target
 * document is left as it is, so a merge never overwrites decisions taken in the target document.
 */
public class DocumentLayoutSyncService {

    /**
     * @param copiedLayoutTypeIds IDs of the types whose layout was copied into the target document
     * @param allowedTypeIds      IDs of the types which were added to the target document's list of allowed types
     */
    public record LayoutSyncResult(@NotNull List<String> copiedLayoutTypeIds, @NotNull List<String> allowedTypeIds) {
    }

    /**
     * Copies into the target document the layouts of those work item types which the source document configures
     * and the target one does not, and allows these types in the target document.
     */
    public @NotNull LayoutSyncResult copyMissingLayouts(@NotNull IModule sourceModule, @NotNull IModule targetModule, @NotNull Set<String> workItemTypeIds) {
        List<String> copiedTypeIds = new ArrayList<>();
        for (String typeId : workItemTypeIds) {
            IModule.IRenderingLayoutStruct sourceLayout = findLayout(sourceModule, typeId);
            if (sourceLayout != null && isUsable(sourceLayout) && findLayout(targetModule, typeId) == null) {
                copyLayout(sourceLayout, targetModule);
                copiedTypeIds.add(typeId);
            }
        }
        return new LayoutSyncResult(copiedTypeIds, allowWorkItemTypes(targetModule, workItemTypeIds));
    }

    @VisibleForTesting
    @Nullable
    IModule.IRenderingLayoutStruct findLayout(@NotNull IModule module, @NotNull String typeId) {
        List<IModule.IRenderingLayoutStruct> layouts = module.getRenderingLayouts();
        return layouts == null ? null : layouts.stream().filter(layout -> typeId.equals(layout.getType())).findFirst().orElse(null);
    }

    /**
     * A layout without a layouter is one the target document could not render, and a document Polarion cannot
     * render is a document it refuses to open. Such a layout is left where it is rather than copied.
     */
    @VisibleForTesting
    boolean isUsable(@NotNull IModule.IRenderingLayoutStruct layout) {
        return !isBlank(layout.getType()) && !isBlank(layout.getLayouter());
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    @VisibleForTesting
    void copyLayout(@NotNull IModule.IRenderingLayoutStruct sourceLayout, @NotNull IModule targetModule) {
        IModule.IRenderingLayoutStruct targetLayout = targetModule.addRenderingLayout(sourceLayout.getLabel(), sourceLayout.getType(), sourceLayout.getLayouter());
        for (IStructure property : properties(sourceLayout)) {
            Object key = property.getValue(IModule.IRenderingLayoutStruct.KEY_PROPERTIES_KEY);
            Object value = property.getValue(IModule.IRenderingLayoutStruct.KEY_PROPERTIES_VALUE);
            if (key != null) {
                targetLayout.setProperty(String.valueOf(key), value == null ? null : String.valueOf(value));
            }
        }
    }

    /**
     * Properties of a layout are a collection of key/value structures, so they can be copied as a whole
     * without knowing which properties the layouter of a certain type supports.
     */
    @SuppressWarnings("unchecked")
    private @NotNull Collection<IStructure> properties(@NotNull IModule.IRenderingLayoutStruct layout) {
        Object properties = layout.getValue(IModule.IRenderingLayoutStruct.KEY_PROPERTIES);
        return properties instanceof Collection<?> collection ? (Collection<IStructure>) collection : List.of();
    }

    /**
     * Extends the document's list of allowed work item types. An empty list means that all types are allowed,
     * so in that case nothing has to be done.
     * <p>
     * The types are added to the list the document already holds. {@code allowedWITypes} is a list field
     * ({@code Module.getAllowedWITypes} is {@code getValue("allowedWITypes")}), and a list field is never written
     * as a whole - which is also how {@code IModule.addRenderingLayout} puts a layout into its own list.
     *
     * @return IDs of the types which were added
     */
    @VisibleForTesting
    @NotNull
    List<String> allowWorkItemTypes(@NotNull IModule targetModule, @NotNull Set<String> workItemTypeIds) {
        List<ITypeOpt> allowedTypes = targetModule.getAllowedWITypes();
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return List.of();
        }
        List<String> allowedTypeIds = allowedTypes.stream().map(ITypeOpt::getId).toList();
        List<ITypeOpt> typesToAdd = workItemTypeIds.stream()
                .filter(typeId -> !allowedTypeIds.contains(typeId))
                .map(typeId -> targetModule.getProject().getWorkItemTypeEnum().wrapOption(typeId))
                .filter(Objects::nonNull)
                .toList();
        typesToAdd.forEach(allowedTypes::add);
        return typesToAdd.stream().map(ITypeOpt::getId).toList();
    }
}

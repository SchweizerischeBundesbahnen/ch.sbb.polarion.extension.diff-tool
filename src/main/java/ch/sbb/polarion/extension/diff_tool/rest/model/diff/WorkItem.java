package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.data.model.IType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a WorkItem")
public final class WorkItem {
    @JsonIgnore
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private IWorkItem underlyingObject;

    @JsonIgnore
    @ToString.Exclude
    private IModule module;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<String, Field> fieldsMap = new HashMap<>();

    @Schema(description = "Unique identifier of the WorkItem")
    private String id;

    @Schema(description = "Title of the WorkItem")
    private String title;

    @Schema(description = "Outline number of the WorkItem")
    private String outlineNumber;

    @Schema(description = "Project ID associated with the WorkItem")
    private String projectId;

    @Schema(description = "Flag indicating whether the WorkItem is referenced")
    private boolean referenced;

    @Schema(description = "Revision number of the WorkItem")
    private String revision;

    @Schema(description = "Last revision number of the WorkItem")
    private String lastRevision;

    @Schema(description = "Moved outline number of the work item after a merge")
    private String movedOutlineNumber;

    @Schema(description = "Direction of the merge move", implementation = MergeMoveDirection.class)
    private MergeMoveDirection moveDirection;

    @Schema(description = "Flag indicating whether the WorkItem is assigned to a different project")
    private boolean externalProjectWorkItem;


    private WorkItem(@NotNull IWorkItem underlyingObject, String outlineNumber, boolean referenced, boolean externalProjectWorkItem) {
        this.underlyingObject = underlyingObject;
        id = underlyingObject.getId();
        title = underlyingObject.getTitle();
        this.outlineNumber = outlineNumber;
        this.projectId = underlyingObject.getProjectId();
        this.referenced = referenced;
        this.revision = underlyingObject.getRevision();
        this.externalProjectWorkItem = externalProjectWorkItem;
        try {
            this.lastRevision = underlyingObject.getLastRevision();
        } catch (Exception e) {
            // getLastRevision() throws an exception for newly created work item on merge, seems that it's connected
            // with the non-completed transaction. Fast workaround here - just ignore it,
            // this data isn't necessary at the post-merge stage.
        }
    }

    public static WorkItem of(@Nullable IWorkItem iWorkItem, String outlineNumber, boolean referenced, boolean externalProjectWorkItem) {
        return iWorkItem == null ? null : new WorkItem(iWorkItem, outlineNumber, referenced, externalProjectWorkItem);
    }

    public static WorkItem of(@Nullable IWorkItem iWorkItem) {
        return iWorkItem == null ? null : new WorkItem(iWorkItem, null, false, false);
    }

    public static WorkItem of(@NotNull WorkItem workItem) {
        return new WorkItem(workItem.getUnderlyingObject(), workItem.getOutlineNumber(), workItem.isReferenced(), workItem.isExternalProjectWorkItem());
    }

    @JsonIgnore
    public IModule getModule() {
        return module != null ? module : underlyingObject.getModule();
    }

    @JsonIgnore
    public boolean sameDocument(WorkItem another) {
        return another != null && Objects.equals(getProjectId(), another.getProjectId())
                && getModule() != null && another.getModule() != null
                && Objects.equals(getModule().getCreated(), another.getModule().getCreated());
    }

    public void addField(@NotNull Field field) {
        fieldsMap.put(field.getId(), field);
    }

    @Nullable
    public Field getField(@NotNull String fieldId) {
        return fieldsMap.get(fieldId);
    }

    public Collection<Field> getFields() {
        return fieldsMap.values();
    }

    @Data
    @Builder
    @Schema(description = "Represents a field within a work item")
    public static class Field {

        @Schema(description = "Unique identifier of the field")
        private String id;

        @Schema(description = "Name of the field")
        private String name;

        @Schema(description = "Type of the field")
        private IType type;

        @Schema(description = "Value of the field")
        private Object value;

        @Schema(description = "HTML representation of the field")
        private String html;

        @Schema(description = "HTML diff representation of the field")
        private String htmlDiff;

        @Schema(description = "Set of issues related to the field")
        public final Set<String> issues = new HashSet<>();

        public void addIssue(String issue) {
            issues.add(issue);
        }
    }
}

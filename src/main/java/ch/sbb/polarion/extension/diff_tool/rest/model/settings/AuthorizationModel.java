package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.polarion.core.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationModel extends SettingsModel {
    public static final String GLOBAL_ROLES = "globalRoles";
    public static final String PROJECT_ROLES = "projectRoles";

    protected List<String> globalRoles;
    protected List<String> projectRoles;

    public void setGlobalRoles(String... roles) {
        globalRoles = Arrays.asList(roles);
    }

    public void setProjectRoles(String... roles) {
        projectRoles = Arrays.asList(roles);
    }

    @Override
    protected String serializeModelData() {
        return serializeEntry(GLOBAL_ROLES, serializeRoles(globalRoles)) +
                serializeEntry(PROJECT_ROLES, serializeRoles(projectRoles));
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        globalRoles = deserializeRoles(GLOBAL_ROLES, serializedString);
        projectRoles = deserializeRoles(PROJECT_ROLES, serializedString);
    }

    @NotNull
    protected String serializeRoles(@Nullable List<String> roles) {
        return roles == null ? "" : String.join(",", roles);
    }

    @NotNull
    protected List<String> deserializeRoles(@NotNull String what, @NotNull String serializedString) {
        final String roles = deserializeEntry(what, serializedString);
        return Arrays.stream(roles.split(",")).filter(s -> !StringUtils.isEmpty(s)).map(String::trim).toList();
    }

    @JsonIgnore
    public List<String> getAllRoles() {
        List<String> roles = new ArrayList<>();
        roles.addAll(getGlobalRoles());
        roles.addAll(getProjectRoles());
        return roles;
    }
}

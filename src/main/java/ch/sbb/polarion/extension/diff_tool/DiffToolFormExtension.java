package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.IFormExtensionContext;
import com.polarion.platform.persistence.model.IPObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DiffToolFormExtension implements IFormExtension {
    private static final String OPTION_TEMPLATE = "<option value='%s' %s>%s</option>";
    private static final String SELECTED = "selected";

    private static final String PROJECT_OPTIONS_PLACEHOLDER = "{PROJECT_OPTIONS}";
    private static final String LINK_ROLE_OPTIONS_PLACEHOLDER = "{LINK_ROLE_OPTIONS}";
    private static final String CONFIG_OPTIONS_PLACEHOLDER = "{CONFIG_OPTIONS}";
    private static final String SOURCE_DOC_PARAMS_PLACEHOLDER = "{SOURCE_DOC_PARAMS}";

    PolarionService polarionService = new PolarionService();

    @Override
    @Nullable
    public String render(@NotNull IFormExtensionContext context) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> renderForm(transaction.context(), context.object().getOldApi()));
    }

    private @NotNull String renderForm(@NotNull SharedContext context, @NotNull IPObject object) {
        HtmlFragmentBuilder builder = context.createHtmlFragmentBuilderFor().gwt();

        if (object instanceof IModule module) {
            String form = ScopeUtils.getFileContent("webapp/diff-tool/html/diff-tool.html");

            List<IProject> allProjects = polarionService.getAuthorizedProjects();
            form = form.replace(PROJECT_OPTIONS_PLACEHOLDER, "<option disabled selected value> --- select project --- </option>" + allProjects.stream().map(this::getProjectOption).collect(Collectors.joining()));

            Collection<ILinkRoleOpt> linkRoles = polarionService.getLinkRoles(module.getProjectId());
            form = form.replace(LINK_ROLE_OPTIONS_PLACEHOLDER, linkRoles.stream().map(this::getLinkRoleOption).collect(Collectors.joining()));

            Collection<SettingName> settingNames = getSettingNames(DiffSettings.FEATURE_NAME, ScopeUtils.getScopeFromProject(module.getProjectId()));
            form = form.replace(CONFIG_OPTIONS_PLACEHOLDER, generateSettingOptions(settingNames));

            String params = fillParams(module.getProjectId(), module.getModuleFolder(), module.getModuleName(), Objects.requireNonNullElse(module.getRevision(), ""));
            // replace with the string like "e-library","specification","Product Specification",null
            form = form.replace(SOURCE_DOC_PARAMS_PLACEHOLDER, params);

            builder.html(form);
        }

        builder.finished();
        return builder.toString();
    }

    private @NotNull String getProjectOption(@NotNull IProject project) {
        String projectId = project.getId();
        String projectName = project.getName();
        String displayOption = projectName != null ? projectName : projectId;
        return String.format(OPTION_TEMPLATE, projectId, "", displayOption);
    }

    private @NotNull String getLinkRoleOption(@NotNull ILinkRoleOpt linkRole) {
        return String.format(OPTION_TEMPLATE, linkRole.getId(), "", String.format("%s / %s", linkRole.getName(), linkRole.getOppositeName()));
    }

    @SuppressWarnings("unchecked")
    private @NotNull Collection<SettingName> getSettingNames(@NotNull String featureName, @NotNull String scope) {
        try {
            return NamedSettingsRegistry.INSTANCE.getByFeatureName(featureName).readNames(scope);
        } catch (IllegalStateException ex) {
            if ("There is already a transaction.".equals(ex.getMessage())) {
                return Collections.emptyList();
            } else {
                throw ex;
            }
        }
    }

    private @NotNull String generateSettingOptions(@NotNull Collection<SettingName> settingNames) {
        if (!settingNames.isEmpty()) {
            String nameToPreselect = settingNames.iterator().next().getName();
            return settingNames.stream()
                    .map(settingName -> String.format(OPTION_TEMPLATE,
                            settingName.getName(), settingName.getName().equals(nameToPreselect) ? SELECTED : "", settingName.getName()))
                    .collect(Collectors.joining());
        } else {
            return String.format(OPTION_TEMPLATE, NamedSettings.DEFAULT_NAME, SELECTED, NamedSettings.DEFAULT_NAME);
        }
    }

    @Override
    @Nullable
    public String getIcon(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return null;
    }

    @Override
    @Nullable
    public String getLabel(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return "Documents Comparison";
    }

    private String fillParams(String... params) {
        return Arrays.stream(params).map(p -> p == null ? null : "\"" + p + "\"").collect(Collectors.joining(","));
    }

}

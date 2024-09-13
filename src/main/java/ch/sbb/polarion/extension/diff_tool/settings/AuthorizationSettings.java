package ch.sbb.polarion.extension.diff_tool.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.settings.AuthorizationModel;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import org.jetbrains.annotations.NotNull;

public class AuthorizationSettings extends GenericNamedSettings<AuthorizationModel> {
    public static final String FEATURE_NAME = "authorization";

    public AuthorizationSettings() {
        super(FEATURE_NAME);
    }

    public AuthorizationSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull AuthorizationModel defaultValues() {
        AuthorizationModel projectCustomFieldsSettingsModel = new AuthorizationModel();
        projectCustomFieldsSettingsModel.setGlobalRoles("admin");
        projectCustomFieldsSettingsModel.setProjectRoles();
        return projectCustomFieldsSettingsModel;
    }
}

package ch.sbb.polarion.extension.diff_tool.rest;

import ch.sbb.polarion.extension.diff_tool.rest.controller.ConversionApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.ConversionInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.DiffApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.DiffInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.MergeApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.MergeInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.UtilityApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.UtilityInternalController;
import ch.sbb.polarion.extension.diff_tool.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public class DiffToolRestApplication extends GenericRestApplication {

    public DiffToolRestApplication() {
        NamedSettingsRegistry.INSTANCE.register(
                Arrays.asList(
                        new DiffSettings(),
                        new AuthorizationSettings()
                )
        );
    }

    @Override
    @NotNull
    public Set<Object> getExtensionControllerSingletons() {
        return Set.of(
                new ConversionApiController(),
                new ConversionInternalController(),
                new DiffApiController(),
                new DiffInternalController(),
                new MergeApiController(),
                new MergeInternalController(),
                new UtilityInternalController(),
                new UtilityApiController()
        );
    }
}

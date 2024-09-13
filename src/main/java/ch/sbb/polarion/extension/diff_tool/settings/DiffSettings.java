package ch.sbb.polarion.extension.diff_tool.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiffSettings extends GenericNamedSettings<DiffModel> {
    public static final String FEATURE_NAME = "diff";

    public static final DiffField TITLE_FIELD = DiffField.builder().key("title").build();
    public static final DiffField DESCRIPTION_FIELD = DiffField.builder().key("description").build();
    public static final DiffField STATUS_FIELD = DiffField.builder().key("status").build();

    public DiffSettings() {
        super(FEATURE_NAME);
    }

    public DiffSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull DiffModel defaultValues() {
        List<DiffField> defaultFields = Arrays.asList(TITLE_FIELD, DESCRIPTION_FIELD, STATUS_FIELD);

        return DiffModel.builder()
                .diffFields(defaultFields)
                .statusesToIgnore(Collections.emptyList())
                .build();
    }
}

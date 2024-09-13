package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
public class DiffModel extends SettingsModel {
    private static final String DIFF_FIELDS = "DIFF FIELDS";
    private static final String STATUSES_TO_IGNORE = "STATUSES TO IGNORE";

    @Builder.Default
    private List<DiffField> diffFields = new ArrayList<>();
    @Builder.Default
    private List<String> statusesToIgnore = new ArrayList<>();

    @Override
    protected String serializeModelData() {
        return serializeEntry(DIFF_FIELDS, diffFields)
                + serializeEntry(STATUSES_TO_IGNORE, statusesToIgnore);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        String fieldsString = deserializeEntry(DIFF_FIELDS, serializedString);
        if (fieldsString != null) {
            try {
                DiffField[] fieldsArray = new ObjectMapper().readValue(fieldsString, DiffField[].class);
                diffFields = new ArrayList<>(Arrays.asList(fieldsArray));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Diff fields value couldn't be parsed", e);
            }
        }

        String statusesString = deserializeEntry(STATUSES_TO_IGNORE, serializedString);
        if (statusesString != null) {
            try {
                String[] statusesArray = new ObjectMapper().readValue(statusesString, String[].class);
                statusesToIgnore = new ArrayList<>(Arrays.asList(statusesArray));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Statuses to ignore value couldn't be parsed", e);
            }
        }
    }

}

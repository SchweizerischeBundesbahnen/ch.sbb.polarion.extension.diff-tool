package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.core.util.logging.Logger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionQueueModel extends SettingsModel {
    private static final Logger logger = Logger.getLogger(ExecutionQueueModel.class);
    private static final String ENTRY_WORKERS = "WORKERS";
    private static final String ENTRY_THREADS = "THREADS";

    @Builder.Default
    private Map<Feature, Integer> workers = new EnumMap<>(Feature.class);

    @Builder.Default
    private Map<Integer, Integer> threads = new HashMap<>();

    @Override
    protected String serializeModelData() {
        return serializeEntry(ENTRY_WORKERS, workers)
                + serializeEntry(ENTRY_THREADS, threads);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        String workersString = deserializeEntry(ENTRY_WORKERS, serializedString);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            workers = objectMapper.readValue(workersString,
                    objectMapper.getTypeFactory().constructMapType(
                            HashMap.class, Feature.class, Integer.class));
        } catch (JsonProcessingException e) {
            workers = new EnumMap<>(Feature.class);
            logger.error("Error deserializing WORKERS data: " + e.getMessage(), e);
        }

        String threadsString = deserializeEntry(ENTRY_THREADS, serializedString);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            threads = objectMapper.readValue(threadsString,
                    objectMapper.getTypeFactory().constructMapType(
                            HashMap.class, Integer.class, Integer.class));
        } catch (JsonProcessingException e) {
            threads = new HashMap<>();
            logger.error("Error deserializing THREADS data: " + e.getMessage(), e);
        }
    }

}

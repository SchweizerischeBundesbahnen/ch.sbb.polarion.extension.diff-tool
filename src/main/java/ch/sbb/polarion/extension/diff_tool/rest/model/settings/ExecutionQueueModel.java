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
    private static final String WORKERS = "WORKERS";
    private static final String THREADS = "THREADS";

    @Builder.Default
    private Map<Feature, Integer> workers = new HashMap<>();

    @Builder.Default
    private Map<Integer, Integer> threads = new HashMap<>();

    @Override
    protected String serializeModelData() {
        return serializeEntry(WORKERS, workers)
                + serializeEntry(THREADS, threads);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        String workersString = deserializeEntry(WORKERS, serializedString);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            workers = objectMapper.readValue(workersString,
                    objectMapper.getTypeFactory().constructMapType(
                            HashMap.class, Feature.class, Integer.class));
        } catch (JsonProcessingException e) {
            workers = new HashMap<>();
            logger.error("Error deserializing WORKERS data: " + e.getMessage(), e);
        }

        String threadsString = deserializeEntry(THREADS, serializedString);
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

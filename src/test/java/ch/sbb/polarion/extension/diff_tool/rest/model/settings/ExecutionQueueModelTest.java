package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionQueueModelTest {

    private static Stream<Arguments> testValuesForQueueModel() {
        return Stream.of(
                Arguments.of(String.format("ok file" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "12345%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN WORKERS-----%1$s" +
                                "{\"DIFF_TEXT\":1,\"DIFF_HTML\":3}%1$s" +
                                "-----END WORKERS-----%1$s" +
                                "-----BEGIN THREADS-----%1$s" +
                                "{\"1\":1,\"2\":6,\"3\":12}%1$s" +
                                "-----END THREADS----- ",
                        System.lineSeparator()), "12345", Map.of(Feature.DIFF_TEXT, 1, Feature.DIFF_HTML, 3), Map.of(1, 1, 2, 6, 3, 12))
        );
    }

    @ParameterizedTest
    @MethodSource("testValuesForQueueModel")
    void getProperExpectedResults(String locationContent, String expectedBundleTimestamp,
                                  Map<Feature, Integer> expectedWorkers, Map<Integer, Integer> expectedThreads) {

        ExecutionQueueModel model = new ExecutionQueueModel();
        model.deserialize(locationContent);

        assertEquals(expectedBundleTimestamp, model.getBundleTimestamp());
        assertEquals(expectedWorkers, model.getWorkers());
        assertEquals(expectedThreads, model.getThreads());
    }

}

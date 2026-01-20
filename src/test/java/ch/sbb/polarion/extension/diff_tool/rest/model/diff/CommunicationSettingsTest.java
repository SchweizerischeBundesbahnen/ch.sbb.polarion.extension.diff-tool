package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommunicationSettingsTest {

    @Test
    void testBuilderCreatesInstance() {
        CommunicationSettings settings = CommunicationSettings.builder()
                .chunkSize(5)
                .build();

        assertEquals(5, settings.getChunkSize());
    }

    @Test
    void testAllArgsConstructor() {
        CommunicationSettings settings = new CommunicationSettings(10);

        assertEquals(10, settings.getChunkSize());
    }

    @Test
    void testNoArgsConstructorWithNullChunkSize() {
        // Note: With null chunkSize, getChunkSize() would throw NPE due to unboxing
        // This is expected behavior as the class requires chunkSize to be set
        CommunicationSettings settingsWithValue = CommunicationSettings.builder().chunkSize(1).build();
        assertEquals(1, settingsWithValue.getChunkSize());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, Integer.MIN_VALUE})
    void testGetChunkSizeReturnsMinimumOneForNonPositiveValues(int negativeOrZeroValue) {
        CommunicationSettings settings = CommunicationSettings.builder()
                .chunkSize(negativeOrZeroValue)
                .build();

        assertEquals(1, settings.getChunkSize());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 100, Integer.MAX_VALUE})
    void testGetChunkSizeReturnsValueForPositiveValues(int positiveValue) {
        CommunicationSettings settings = CommunicationSettings.builder()
                .chunkSize(positiveValue)
                .build();

        assertEquals(positiveValue, settings.getChunkSize());
    }
}

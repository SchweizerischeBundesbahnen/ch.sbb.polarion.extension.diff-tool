package ch.sbb.polarion.extension.diff_tool.rest.model.conversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrintOrientationTest {

    @Test
    void testEnumValues() {
        PrintOrientation[] values = PrintOrientation.values();

        assertEquals(2, values.length);
        assertEquals(PrintOrientation.PORTRAIT, values[0]);
        assertEquals(PrintOrientation.LANDSCAPE, values[1]);
    }

    @Test
    void testValueOf() {
        assertEquals(PrintOrientation.PORTRAIT, PrintOrientation.valueOf("PORTRAIT"));
        assertEquals(PrintOrientation.LANDSCAPE, PrintOrientation.valueOf("LANDSCAPE"));
    }

    @Test
    void testToCssStringPortrait() {
        assertEquals("portrait", PrintOrientation.PORTRAIT.toCssString());
    }

    @Test
    void testToCssStringLandscape() {
        assertEquals("landscape", PrintOrientation.LANDSCAPE.toCssString());
    }

    @ParameterizedTest
    @CsvSource({
            "portrait, PORTRAIT",
            "PORTRAIT, PORTRAIT",
            "Portrait, PORTRAIT",
            "landscape, LANDSCAPE",
            "LANDSCAPE, LANDSCAPE",
            "Landscape, LANDSCAPE"
    })
    void testFromStringValidValues(String input, PrintOrientation expected) {
        assertEquals(expected, PrintOrientation.fromString(input));
    }

    @ParameterizedTest
    @NullSource
    void testFromStringNullReturnsNull(String input) {
        assertNull(PrintOrientation.fromString(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "PORT", "LAND", "", " ", "portrait ", " portrait"})
    void testFromStringInvalidValuesThrowsWebApplicationException(String input) {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> PrintOrientation.fromString(input));

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Unsupported value for orientation parameter: " + input,
                exception.getResponse().getEntity());
    }
}

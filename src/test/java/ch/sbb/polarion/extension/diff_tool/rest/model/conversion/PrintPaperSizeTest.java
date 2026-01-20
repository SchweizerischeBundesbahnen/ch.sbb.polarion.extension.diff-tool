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

class PrintPaperSizeTest {

    @Test
    void testEnumValues() {
        PrintPaperSize[] values = PrintPaperSize.values();

        assertEquals(2, values.length);
        assertEquals(PrintPaperSize.A4, values[0]);
        assertEquals(PrintPaperSize.A3, values[1]);
    }

    @Test
    void testValueOf() {
        assertEquals(PrintPaperSize.A4, PrintPaperSize.valueOf("A4"));
        assertEquals(PrintPaperSize.A3, PrintPaperSize.valueOf("A3"));
    }

    @ParameterizedTest
    @CsvSource({
            "a4, A4",
            "A4, A4",
            "a3, A3",
            "A3, A3"
    })
    void testFromStringValidValues(String input, PrintPaperSize expected) {
        assertEquals(expected, PrintPaperSize.fromString(input));
    }

    @ParameterizedTest
    @NullSource
    void testFromStringNullReturnsNull(String input) {
        assertNull(PrintPaperSize.fromString(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "A5", "letter", "", " ", "a4 ", " a4"})
    void testFromStringInvalidValuesThrowsWebApplicationException(String input) {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> PrintPaperSize.fromString(input));

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Unsupported value for paperSize parameter: " + input,
                exception.getResponse().getEntity());
    }
}

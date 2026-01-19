package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeDirectionTest {

    @Test
    void testEnumValues() {
        MergeDirection[] values = MergeDirection.values();

        assertEquals(2, values.length);
        assertEquals(MergeDirection.LEFT_TO_RIGHT, values[0]);
        assertEquals(MergeDirection.RIGHT_TO_LEFT, values[1]);
    }

    @Test
    void testValueOf() {
        assertEquals(MergeDirection.LEFT_TO_RIGHT, MergeDirection.valueOf("LEFT_TO_RIGHT"));
        assertEquals(MergeDirection.RIGHT_TO_LEFT, MergeDirection.valueOf("RIGHT_TO_LEFT"));
    }

    @Test
    void testOrdinal() {
        assertEquals(0, MergeDirection.LEFT_TO_RIGHT.ordinal());
        assertEquals(1, MergeDirection.RIGHT_TO_LEFT.ordinal());
    }
}

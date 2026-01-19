package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeMoveDirectionTest {

    @Test
    void testEnumValues() {
        MergeMoveDirection[] values = MergeMoveDirection.values();

        assertEquals(2, values.length);
        assertEquals(MergeMoveDirection.UP, values[0]);
        assertEquals(MergeMoveDirection.DOWN, values[1]);
    }

    @Test
    void testValueOf() {
        assertEquals(MergeMoveDirection.UP, MergeMoveDirection.valueOf("UP"));
        assertEquals(MergeMoveDirection.DOWN, MergeMoveDirection.valueOf("DOWN"));
    }

    @Test
    void testOrdinal() {
        assertEquals(0, MergeMoveDirection.UP.ordinal());
        assertEquals(1, MergeMoveDirection.DOWN.ordinal());
    }

    @Test
    void testGetOppositeOfUp() {
        assertEquals(MergeMoveDirection.DOWN, MergeMoveDirection.UP.getOpposite());
    }

    @Test
    void testGetOppositeOfDown() {
        assertEquals(MergeMoveDirection.UP, MergeMoveDirection.DOWN.getOpposite());
    }

    @Test
    void testGetOppositeIsSymmetric() {
        assertEquals(MergeMoveDirection.UP, MergeMoveDirection.UP.getOpposite().getOpposite());
        assertEquals(MergeMoveDirection.DOWN, MergeMoveDirection.DOWN.getOpposite().getOpposite());
    }
}

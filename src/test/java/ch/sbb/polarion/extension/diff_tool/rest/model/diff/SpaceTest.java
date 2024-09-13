package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaceTest {
    @Test
    void testName() {
        Space space = new Space("Some name");
        assertEquals("Some name", space.getName());
    }

    @Test
    void testDefaultName() {
        Space space = new Space(Space.DEFAULT_SPACE_ID);
        assertEquals(Space.DEFAULT_SPACE_NAME, space.getName());
    }

    @Test
    void testCompare() {
        Space space1 = new Space("space1");
        Space space2 = new Space("space2");
        assertEquals("space1".compareTo("space2"), space1.compareTo(space2));
    }
}

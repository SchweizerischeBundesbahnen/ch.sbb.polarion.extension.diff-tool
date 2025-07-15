package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkRoleTest {

    @Test
    void testGetCombinedId() {
        assertEquals("type#role", new LinkRole("role", "name", "type", "typeName").getCombinedId());
    }
}

package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkRoleDirectionTest {

    @Test
    void testValueOf() {
        assertEquals(LinkRoleDirection.DIRECT, LinkRoleDirection.valueOf("DIRECT"));
        assertEquals(LinkRoleDirection.REVERSE, LinkRoleDirection.valueOf("REVERSE"));
    }

    @Test
    void testFromValueWithValidValues() {
        assertEquals(LinkRoleDirection.DIRECT, LinkRoleDirection.fromValue("DIRECT"));
        assertEquals(LinkRoleDirection.REVERSE, LinkRoleDirection.fromValue("REVERSE"));
    }

    @Test
    void testFromValueCaseInsensitive() {
        assertEquals(LinkRoleDirection.DIRECT, LinkRoleDirection.fromValue("direct"));
        assertEquals(LinkRoleDirection.REVERSE, LinkRoleDirection.fromValue("reverse"));
        assertEquals(LinkRoleDirection.DIRECT, LinkRoleDirection.fromValue("Direct"));
    }

    @Test
    void testFromValueReturnsDirectForNull() {
        assertEquals(LinkRoleDirection.DIRECT, LinkRoleDirection.fromValue(null));
    }

    @Test
    void testFromValueReturnsDirectForEmptyString() {
        assertEquals(LinkRoleDirection.DIRECT, LinkRoleDirection.fromValue(""));
    }

    @Test
    void testFromValueReturnsDirectForBlankString() {
        assertEquals(LinkRoleDirection.DIRECT, LinkRoleDirection.fromValue("   "));
    }

    @Test
    void testFromValueThrowsForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> LinkRoleDirection.fromValue("INVALID"));
    }
}

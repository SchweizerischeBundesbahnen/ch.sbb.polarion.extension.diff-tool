package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.projects.model.IFolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void testTitleDefaultsToId() {
        Space space = new Space("Specification");
        assertEquals("Specification", space.getTitle());
    }

    @Test
    void testFromFolderWithTitle() {
        IFolder folder = folder("Specification", "Specifications");
        Space space = Space.from(folder);
        assertEquals("Specification", space.getId());
        assertEquals("Specifications", space.getTitle());
    }

    @Test
    void testFromFolderWithoutTitle() {
        IFolder blank = folder("Blank", " ");
        IFolder missing = folder("Missing", null);
        assertEquals("Blank", Space.from(blank).getTitle());
        assertEquals("Missing", Space.from(missing).getTitle());
    }

    @Test
    void testCompare() {
        Space space1 = new Space("space1");
        Space space2 = new Space("space2");
        assertEquals("space1".compareTo("space2"), space1.compareTo(space2));
    }

    private IFolder folder(String name, String title) {
        IFolder folder = mock(IFolder.class);
        when(folder.getName()).thenReturn(name);
        when(folder.getTitleOrName()).thenReturn(title);
        return folder;
    }
}

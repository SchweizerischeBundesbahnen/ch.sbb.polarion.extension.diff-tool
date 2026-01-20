package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectTest {

    @Test
    void testNoArgsConstructor() {
        Project project = new Project();
        assertNull(project.getId());
        assertNull(project.getName());
        assertFalse(project.isAuthorizedForMerge());
    }

    @Test
    void testAllArgsConstructor() {
        Project project = new Project("projectId", "projectName", true);

        assertEquals("projectId", project.getId());
        assertEquals("projectName", project.getName());
        assertTrue(project.isAuthorizedForMerge());
    }

    @Test
    void testBuilder() {
        Project project = Project.builder()
                .id("projectId")
                .name("projectName")
                .authorizedForMerge(true)
                .build();

        assertEquals("projectId", project.getId());
        assertEquals("projectName", project.getName());
        assertTrue(project.isAuthorizedForMerge());
    }

    @Test
    void testSetters() {
        Project project = new Project();

        project.setId("newId");
        project.setName("newName");
        project.setAuthorizedForMerge(true);

        assertEquals("newId", project.getId());
        assertEquals("newName", project.getName());
        assertTrue(project.isAuthorizedForMerge());
    }

    @Test
    void testAuthorizedForMergeFalse() {
        Project project = Project.builder()
                .id("projectId")
                .authorizedForMerge(false)
                .build();

        assertFalse(project.isAuthorizedForMerge());
    }

    @Test
    void testEqualsAndHashCode() {
        Project project1 = new Project("id", "name", true);
        Project project2 = new Project("id", "name", true);
        Project project3 = new Project("differentId", "name", true);

        assertEquals(project1, project2);
        assertEquals(project1.hashCode(), project2.hashCode());
        assertNotEquals(project1, project3);
    }

    @Test
    void testToString() {
        Project project = new Project("id", "name", true);

        String toString = project.toString();

        assertTrue(toString.contains("id"));
        assertTrue(toString.contains("name"));
        assertTrue(toString.contains("authorizedForMerge"));
    }
}

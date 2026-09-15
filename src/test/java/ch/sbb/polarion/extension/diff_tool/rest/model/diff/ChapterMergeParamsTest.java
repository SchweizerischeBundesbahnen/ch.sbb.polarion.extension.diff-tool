package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterMergeParamsTest {

    @Test
    void testAnOmittedPropertyMeansWhatTheSchemaSaysItMeans() throws Exception {
        ChapterMergeParams params = new ObjectMapper().readValue("""
                {"sourceChapterOutlineNumber": "2", "targetChapterOutlineNumber": "3.1"}
                """, ChapterMergeParams.class);

        // a client of the API reads these defaults from the published schema, so they are part of the contract
        assertEquals(documentedDefault("copyWorkItemLayouts"), String.valueOf(params.isCopyWorkItemLayouts()));
        assertEquals(documentedDefault("referencedItems"), params.getReferencedItems().name());
    }

    @Test
    void testWorkItemLayoutsAreCopiedUnlessTheyAreSwitchedOff() {
        // a copy is rendered by the layout its type has in the document it lands in, whichever way it is built
        assertTrue(new ChapterMergeParams().isCopyWorkItemLayouts());
        assertTrue(ChapterMergeParams.builder().build().isCopyWorkItemLayouts());
    }

    private String documentedDefault(String fieldName) throws NoSuchFieldException {
        return ChapterMergeParams.class.getDeclaredField(fieldName).getAnnotation(Schema.class).defaultValue();
    }
}

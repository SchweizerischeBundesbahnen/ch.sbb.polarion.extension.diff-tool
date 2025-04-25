package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsMergeContext;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiffModelTest {

    private static Stream<Arguments> testValuesForDiffModel() {
        return Stream.of(
                Arguments.of(null, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                Arguments.of("", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                Arguments.of("some badly formatted string", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                Arguments.of(String.format("ok file" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "12345%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN DIFF FIELDS-----%1$s" +
                                "[{\"key\":\"status\"},{\"key\":\"title\"}]%1$s" +
                                "-----END DIFF FIELDS-----%1$s" +
                                "-----BEGIN STATUSES TO IGNORE-----%1$s" +
                                "[\"inprogress\"]%1$s" +
                                "-----END STATUSES TO IGNORE-----%1$s" +
                                "-----BEGIN HYPERLINK ROLES-----%1$s" +
                                "[\"ref_int\"]%1$s" +
                                "-----END HYPERLINK ROLES-----%1$s",
                        System.lineSeparator()), "12345", Arrays.asList(DiffField.builder().key("status").build(), DiffField.builder().key("title").build()), Collections.singletonList("inprogress"), Collections.singletonList("ref_int")),
                Arguments.of(String.format("no bundle timestamp" +
                                "-----BEGIN DIFF FIELDS-----%1$s" +
                                "[{\"key\":\"title\",\"wiTypeId\":\"assumption\"}]" +
                                "-----END DIFF FIELDS-----%1$s" +
                                "-----BEGIN STATUSES TO IGNORE-----%1$s" +
                                "[\"draft\",\"open\"]%1$s" +
                                "-----END STATUSES TO IGNORE-----%1$s",
                        System.lineSeparator()), null, Collections.singletonList(DiffField.builder().key("title").wiTypeId("assumption").build()), Arrays.asList("draft", "open"), List.of()),
                Arguments.of(String.format("keep first duplicated entry" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "ts1%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "ts2%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN DIFF FIELDS-----%1$s" +
                                "[{\"key\":\"status\"},{\"key\":\"title\"}]" +
                                "-----END DIFF FIELDS-----%1$s" +
                                "-----BEGIN DIFF FIELDS-----%1$s" +
                                "[{\"key\":\"title\",\"wiTypeId\":\"assumption\"}]" +
                                "-----END DIFF FIELDS-----%1$s" +
                                "-----BEGIN STATUSES TO IGNORE-----%1$s" +
                                "[\"draft\",\"open\"]%1$s" +
                                "-----END STATUSES TO IGNORE-----%1$s" +
                                "-----BEGIN HYPERLINK ROLES-----%1$s" +
                                "[]%1$s" +
                                "-----END HYPERLINK ROLES-----%1$s" +
                                "-----BEGIN STATUSES TO IGNORE-----%1$s" +
                                "[\"inprogress\"]%1$s" +
                                "-----END STATUSES TO IGNORE-----%1$s" +
                                "-----BEGIN HYPERLINK ROLES-----%1$s" +
                                "[\"ref_int\"]%1$s" +
                                "-----END HYPERLINK ROLES-----%1$s",
                        System.lineSeparator()), "ts1", Arrays.asList(DiffField.builder().key("status").build(), DiffField.builder().key("title").build()), Arrays.asList("draft", "open"), List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("testValuesForDiffModel")
    void getProperExpectedResults(String locationContent, String expectedBundleTimestamp,
                                  List<DiffField> expectedFields, List<String> expectedStatusesToIgnore, List<String> expectedHyperlinkRoles) {

        DiffModel model = new DiffModel();
        model.deserialize(locationContent);

        assertEquals(expectedBundleTimestamp, model.getBundleTimestamp());
        assertEquals(expectedFields, model.getDiffFields());
        assertEquals(expectedStatusesToIgnore, model.getStatusesToIgnore());
        assertEquals(expectedHyperlinkRoles, model.getHyperlinkRoles());
    }

    @Test
    void testRoleNotFound() {
        PolarionService polarionService = mock(PolarionService.class);

        DocumentIdentifier leftIdentifier = mock(DocumentIdentifier.class);
        DocumentIdentifier rightIdentifier = mock(DocumentIdentifier.class);

        IModule leftModule = mock(IModule.class);
        IModule rightModule = mock(IModule.class);
        when(polarionService.getModule(leftIdentifier)).thenReturn(leftModule);
        when(polarionService.getModule(rightIdentifier)).thenReturn(rightModule);

        DiffModel model = mock(DiffModel.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new DocumentsMergeContext(polarionService, leftIdentifier, rightIdentifier, MergeDirection.LEFT_TO_RIGHT, "someLinkRole", model, true));
        assertEquals("No link role could be found by ID 'someLinkRole'", exception.getMessage());
    }
}

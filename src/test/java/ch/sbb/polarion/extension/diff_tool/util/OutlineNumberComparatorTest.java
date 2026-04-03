package ch.sbb.polarion.extension.diff_tool.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutlineNumberComparatorTest {

    private final OutlineNumberComparator comparator = new OutlineNumberComparator();

    @Test
    void testOutlineNumbersOrder() {
        ArrayList<String> inputList = new ArrayList<>(Arrays.asList(
                "3",
                "2",
                "25",
                "1.24.1-1",
                "1.24.2",
                "1.24.3-1",
                "1.2.11-1",
                "1.2.11-2",
                "1.2.11-3",
                "1.1",
                "1.1-3",
                "1.1-3.23",
                "1.1-3.1",
                "1.1-3.1.4",
                "1.1-3.3",
                "1.1.1",
                "1.1-46",
                "1.2.11-25",
                "1.24.16-3"
        ));

        inputList.sort(comparator);

        assertThat(inputList).isEqualTo(Arrays.asList(
                "1.1",
                "1.1-3",
                "1.1-3.1",
                "1.1-3.1.4",
                "1.1-3.3",
                "1.1-3.23",
                "1.1-46",
                "1.1.1",
                "1.2.11-1",
                "1.2.11-2",
                "1.2.11-3",
                "1.2.11-25",
                "1.24.1-1",
                "1.24.2",
                "1.24.3-1",
                "1.24.16-3",
                "2",
                "3",
                "25"
        ));
    }

    @Test
    void testEqualOutlineNumbers() {
        assertEquals(0, comparator.compare("1.2.3", "1.2.3"));
        assertEquals(0, comparator.compare("1", "1"));
        assertEquals(0, comparator.compare("1.2-3", "1.2-3"));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 2",
            "1.1, 1.2",
            "1.1.1, 1.1.2",
            "1.1, 1.1.1",
            "1.1-1, 1.1-2",
            "1.1, 1.1-1",
    })
    void testLessThanComparisons(String smaller, String larger) {
        assertTrue(comparator.compare(smaller, larger) < 0, smaller + " should be less than " + larger);
        assertTrue(comparator.compare(larger, smaller) > 0, larger + " should be greater than " + smaller);
    }

    @Test
    void testSingleSegmentNumbers() {
        ArrayList<String> numbers = new ArrayList<>(Arrays.asList("10", "2", "1", "20", "3"));
        numbers.sort(comparator);
        assertThat(numbers).containsExactly("1", "2", "3", "10", "20");
    }

    @Test
    void testWithoutSuffix_prefixIsParentOfLonger() {
        assertTrue(comparator.compare("1.2", "1.2.3") < 0);
        assertTrue(comparator.compare("1.2.3", "1.2") > 0);
    }

    @Test
    void testNonNumericSegment_treatedAsZero() {
        // Non-numeric parts are parsed as 0 via parseLongSafely
        assertEquals(0, comparator.compare("abc", "0"));
    }

    @Test
    void testCachingDoesNotAffectResults() {
        // Use same comparator instance multiple times to exercise cache
        assertTrue(comparator.compare("1.1", "1.2") < 0);
        assertTrue(comparator.compare("1.2", "1.3") < 0);
        assertTrue(comparator.compare("1.1", "1.3") < 0);
        assertEquals(0, comparator.compare("1.2", "1.2"));
    }
}

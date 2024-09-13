package ch.sbb.polarion.extension.diff_tool.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OutlineNumberComparatorTest {

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

        inputList.sort(new OutlineNumberComparator());

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
}

package ch.sbb.polarion.extension.diff_tool.util;

import com.polarion.core.util.logging.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutlineNumberComparator implements Comparator<String> {

    private static final Logger logger = Logger.getLogger(OutlineNumberComparator.class);
    private final Map<String, List<Long>> partsCache = new HashMap<>();

    @Override
    public int compare(String outlineNumber1, String outlineNumber2) {
        String[] firstNumberParts = outlineNumber1.split("-");
        String[] secondNumberParts = outlineNumber2.split("-");

        int leftPartsResult = compareDotSeparatedValues(firstNumberParts[0], secondNumberParts[0]);
        if (leftPartsResult != 0) {
            return leftPartsResult;
        } else {
            String firstSubNumber = firstNumberParts.length == 1 ? "0" : firstNumberParts[1];
            String secondSubNumber = secondNumberParts.length == 1 ? "0" : secondNumberParts[1];
            return compareDotSeparatedValues(firstSubNumber, secondSubNumber);
        }
    }

    private int compareDotSeparatedValues(String value1, String value2) {
        List<Long> firstNumbersList = partsCache.computeIfAbsent(value1, v -> Arrays.stream(v.split("\\.")).map(this::parseLongSafely).toList());
        List<Long> secondNumbersList = partsCache.computeIfAbsent(value2, v -> Arrays.stream(v.split("\\.")).map(this::parseLongSafely).toList());

        for (int i = 0; i < firstNumbersList.size(); i++) {
            if (secondNumbersList.size() == i) {
                return 1;
            }
            int currentCompareResult = firstNumbersList.get(i).compareTo(secondNumbersList.get(i));
            if (currentCompareResult != 0) {
                return currentCompareResult;
            }
        }

        return secondNumbersList.size() > firstNumbersList.size() ? -1 : 0;
    }

    private long parseLongSafely(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            logger.warn("Expected number but found: " + s);
            return 0;
        }
    }
}

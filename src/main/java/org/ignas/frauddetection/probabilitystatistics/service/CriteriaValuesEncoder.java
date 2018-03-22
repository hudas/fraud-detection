package org.ignas.frauddetection.probabilitystatistics.service;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CriteriaValuesEncoder {

    public static String encode(Map<String, String> values) {
        return values.entrySet()
            .stream()
            .sorted(((o1, o2) -> o1.getValue().compareToIgnoreCase(o2.getKey())))
            .map(Map.Entry::getValue)
            .map(CriteriaValuesEncoder::shorten)
            .collect(Collectors.joining("."));
    }

    private static String shorten(String value) {
        return Arrays.stream(value.split("_"))
            .map(it -> it.substring(0, 1))
            .collect(Collectors.joining(""));
    }
}

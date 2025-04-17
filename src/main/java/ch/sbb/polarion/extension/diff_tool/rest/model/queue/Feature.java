package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import java.util.List;
import java.util.stream.Stream;

public enum Feature {
    CPU_LOAD,
    DIFF_DOCUMENTS,
    DIFF_DOCUMENT_WORKITEMS,
    DIFF_DOCUMENTS_FIELDS,
    DIFF_DOCUMENTS_CONTENT,
    DIFF_COLLECTIONS,
    DIFF_DETACHED_WORKITEMS,
    DIFF_WORKITEMS_PAIRS,
    DIFF_HTML,
    DIFF_TEXT;

    public static List<Feature> workerFeatures() {
        return Stream.of(values()).filter(feature -> feature != CPU_LOAD).toList();
    }
}

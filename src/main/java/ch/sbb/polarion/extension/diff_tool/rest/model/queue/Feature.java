package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

/**
 * Features whose execution can be assigned to a worker queue, plus the synthetic {@link #CPU_LOAD}
 * series.
 * <p>
 * The label and description are the admin UI's presentation strings and live here on purpose: they used
 * to be a {@code featuresLocalization} map inside the admin page's JavaScript, so adding a constant here
 * left the page showing a raw enum name and throwing on the missing tooltip entry. They are served by
 * {@code GET /queue/configuration-meta}.
 */
@Getter
public enum Feature {
    CPU_LOAD("CPU Load", "Overall CPU load of the machine running Polarion"),
    DIFF_DOCUMENTS("Diff docs", "/diff/documents request: gets difference of two live documents"),
    DIFF_DOCUMENT_WORKITEMS("Diff WI", "/diff/document-workitems request: gets difference of two WorkItems contained in LiveDoc"),
    DIFF_DOCUMENTS_FIELDS("Diff docs fields", "/diff/documents-fields request: gets difference between fields of two live documents"),
    DIFF_DOCUMENTS_CONTENT("Diff docs content", "/diff/documents-content request: gets difference two live documents page content"),
    DIFF_COLLECTIONS("Diff collections", "/diff/collections request: gets difference of two live document collections"),
    DIFF_DETACHED_WORKITEMS("Diff detached WI", "/diff/detached-workitems request: gets difference of two WorkItems not necessarily contained in a document"),
    DIFF_WORKITEMS_PAIRS("Find WI pairs", "/diff/workitems-pairs request: finds pairs to specified WorkItems, for later diff"),
    DIFF_HTML("Diff HTML", "/diff/html request: gets difference of two strings which contain HTML tags"),
    DIFF_TEXT("Diff text", "/diff/text request: gets difference of two strings which contain plain text");

    private final String label;
    private final String description;

    Feature(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public static List<Feature> workerFeatures() {
        return Stream.of(values()).filter(feature -> feature != CPU_LOAD).toList();
    }
}

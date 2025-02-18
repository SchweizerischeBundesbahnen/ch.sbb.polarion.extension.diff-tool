package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DocumentContentProcessor {

    private static final Set<String> HTML_HEADER_TAGS = new HashSet<>(Arrays.asList("h1", "h2", "h3", "h4", "h5"));

    public Map<String, DocumentContentAnchor> process(String documentContent) {
        DocumentContentAnchor lastAnchor = null;
        StringBuilder contentBuffer = new StringBuilder();
        Map<String, DocumentContentAnchor> contentAnchors = new HashMap<>();

        Document doc = Jsoup.parse(documentContent);
        Elements elements = doc.body().children();
        for (Element element : elements) {
            DocumentContentAnchor anchor = anchor(element);
            if (anchor != null) {
                if (!contentBuffer.isEmpty()) {
                    if (lastAnchor == null) {
                        anchor.setContentAbove(contentBuffer.toString());
                    } else if (isChapter(element)) {
                        lastAnchor.setContentBelow(contentBuffer.toString());
                    } else {
                        anchor.setContentAbove(contentBuffer.toString());
                    }
                    contentBuffer = new StringBuilder();
                }
                contentAnchors.put(anchor.getId(), anchor);
                lastAnchor = anchor;
            } else {
                contentBuffer.append(element.outerHtml());
            }
        }
        return contentAnchors;
    }

    private DocumentContentAnchor anchor(Element element) {
        String workItemId = extractWorkItemId(element);
        if (workItemId != null) {
            return DocumentContentAnchor.builder().id(workItemId).build();
        } else {
            return null;
        }
    }

    private boolean isChapter(Element element) {
        return HTML_HEADER_TAGS.contains(element.tagName().toLowerCase());
    }

    private String extractWorkItemId(Element element) {
        if (element.id().contains("name=module-workitem;")) {
            RegexMatcher idMatcher = RegexMatcher.get(".*;params=id=(?<id>[A-Za-z]+-\\d+)");
            return idMatcher.findFirst(element.id(), regexEngine -> regexEngine.group("id")).orElse(null);
        } else {
            return null;
        }
    }

}

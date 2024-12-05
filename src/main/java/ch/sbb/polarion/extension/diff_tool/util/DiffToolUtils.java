package ch.sbb.polarion.extension.diff_tool.util;


import ch.sbb.polarion.extension.diff_tool.rest.model.diff.StringsDiff;
import ch.sbb.polarion.extension.diff_tool.service.ModifiedHtmlSaxDiffOutput;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.logging.Logger;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.Jsoup;
import org.outerj.daisy.diff.helper.NekoHtmlParser;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.InputSource;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public class DiffToolUtils {
    private static final Logger logger = Logger.getLogger(DiffToolUtils.class);
    private static final String OUTER_TAG_NAME = "outer_surrogate_tag";

    @NotNull
    public StringsDiff diffText(@Nullable String text1, @Nullable String text2) {
        return diffHtml(Jsoup.parse(StringUtils.defaultString(text1)).text(), Jsoup.parse(StringUtils.defaultString(text2)).text());
    }

    @NotNull
    public StringsDiff diffHtml(@Nullable String html1, @Nullable String html2) {
        String result = unwrapSurrogateTag(computeDiff(Pair.of(wrapInSurrogateTag(html1), wrapInSurrogateTag(html2))));
        boolean different = isDifferent(result);
        return StringsDiff.builder().different(different).result(different ? applyStyle(result) : result).build();
    }

    @NotNull
    public String computeDiff(@NotNull Pair<String, String> pairToDiff) {
        try {
            StringWriter finalResult = new StringWriter();
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();

            TransformerHandler handler = tf.newTransformerHandler();
            handler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
            handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
            handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            handler.setResult(new StreamResult(finalResult));

            Locale locale = Locale.getDefault();

            NekoHtmlParser parser = new NekoHtmlParser();

            InputSource oldSource = new InputSource(new StringReader(pairToDiff.getLeft()));
            InputSource newSource = new InputSource(new StringReader(pairToDiff.getRight()));

            DomTreeBuilder oldHandler = new DomTreeBuilder();
            parser.parse(oldSource, oldHandler);
            TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale);

            DomTreeBuilder newHandler = new DomTreeBuilder();
            parser.parse(newSource, newHandler);
            TextNodeComparator rightComparator = new TextNodeComparator(newHandler, locale);

            new HTMLDiffer(new ModifiedHtmlSaxDiffOutput(handler, "diff")).diff(leftComparator, rightComparator);

            return finalResult.toString();
        } catch (Exception ex) {
            logger.error("Error occurred while getting HTML diff", ex);
            return "Error occurred while getting HTML diff, please contact system administrator to diagnose the problem";
        }
    }

    /**
     * Used for "missing last part of the diff" issue fix
     *
     * @see ch.sbb.polarion.extension.diff_tool.service.handler.impl.OuterWrapperHandler
     */
    @NotNull
    public String wrapInSurrogateTag(String text) {
        return getOuterTag(false) + StringUtils.defaultString(text) + getOuterTag(true);
    }

    /**
     * Used for "missing last part of the diff" issue fix
     *
     * @see ch.sbb.polarion.extension.diff_tool.service.handler.impl.OuterWrapperHandler
     */
    @NotNull
    public String unwrapSurrogateTag(String text) {
        return text.replace(getOuterTag(true), "").replace(getOuterTag(false), "").trim();
    }

    private String getOuterTag(boolean closing) {
        return (closing ? "</%s>" : "<%s>").formatted(OUTER_TAG_NAME);
    }

    private boolean isDifferent(@NotNull String diffResult) {
        return diffResult.contains("<span class=\"diff-html-");
    }

    public boolean isEnumContainingType(IType fieldType) {
        return fieldType instanceof IEnumType || fieldType instanceof IListType && ((IListType) fieldType).getItemType() instanceof IEnumType;
    }

    @NotNull
    @VisibleForTesting
    String applyStyle(@NotNull String diffResult) {
        Pattern pattern = Pattern.compile("class=\"(diff-html-removed|diff-html-added|diff-html-modified|diff-html-changed)\"");
        Matcher matcher = pattern.matcher(diffResult);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String className = matcher.group(1);
            String style = getStyleByClass(className);
            matcher.appendReplacement(result, "style=\"" + style + "\"");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @NotNull
    @VisibleForTesting
    String getStyleByClass(@NotNull String className) {
        return switch (className) {
            case "diff-html-removed" -> "background-color: #FDC6C6; text-decoration: line-through;";
            case "diff-html-added" -> "background-color: #CCFFCC;";
            case "diff-html-modified" -> "background-color: lightyellow;";
            case "diff-html-changed" -> "border: 1px dashed magenta; padding: 2px; margin: 2px;";
            default -> "";
        };
    }

    /**
     * Get mutable list of linked work items sorted by ID
     */
    public List<ILinkedWorkItemStruct> getLinks(@Nullable IWorkItem workItem, boolean back) {
        return workItem == null ? new ArrayList<>() : (back ? workItem.getLinkedWorkItemsStructsBack() : workItem.getLinkedWorkItemsStructsDirect()).stream()
                .sorted(Comparator.comparing(o -> o.getLinkedItem().getId())).collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean notFromModule(IWorkItem workItem, IModule module) {
        return !Objects.equals(workItem.getProjectId(), module.getProjectId()) ||
                workItem.getModule() == null || // work item may exist outside any document
                !workItem.getModule().getModuleLocation().removeRevision().equals(module.getModuleLocation().removeRevision());
    }

    public boolean sameWorkItem(ILinkedWorkItemStruct linkA, @Nullable IWorkItem itemB) {
        return itemB != null && sameProjectItems(linkA.getLinkedItem(), itemB) && Objects.equals(linkA.getLinkedItem().getId(), itemB.getId());
    }

    public boolean sameProjectItems(@Nullable IWorkItem itemA, @Nullable IWorkItem itemB) {
        return itemA != null && itemB != null && Objects.equals(itemA.getProjectId(), itemB.getProjectId());
    }
}

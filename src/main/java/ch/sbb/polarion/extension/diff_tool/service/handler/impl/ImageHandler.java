package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;

import ch.sbb.polarion.extension.diff_tool.util.ResourceProvider;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.util.DigestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageHandler implements DiffLifecycleHandler {

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        String left = initialPair.getLeft();
        String right = initialPair.getRight();
        if (context.workItemA != null && context.workItemB != null) {
            left = calculateHash(left, context.workItemA);
            right = calculateHash(right, context.workItemB);
        }

        return Pair.of(left, right);
    }

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        // Searches for all WorkItem/Document images enclosed into span-tag containing class starting with "diff-html" and adds this class starting with "diff-html" to the image
        Pattern pattern = Pattern.compile("(?<match><span class=\"(?<diffClass>diff-html-[^\"]+?)\"((?!\">).)*\"><img src=\"(workitemimg|attachment):[^\"]+\")");
        Matcher matcher = pattern.matcher(diff);

        StringBuilder buf = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group("match");
            String diffClass = matcher.group("diffClass");
            matcher.appendReplacement(buf, String.format("%s class=\"%s\"", match, diffClass));
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    @VisibleForTesting
    String calculateHash(String html, WorkItem workItem) {
        // Replace encoded underscore symbol in 'src' attribute of images
        Matcher encodedUnderscoreMatcher = Pattern.compile("(src=\".*?%5F[^\"]*+\")").matcher(html);
        StringBuilder buf = new StringBuilder();
        while (encodedUnderscoreMatcher.find()) {
            String group = encodedUnderscoreMatcher.group();
            encodedUnderscoreMatcher.appendReplacement(buf, group.replace("%5F", "_"));
        }
        encodedUnderscoreMatcher.appendTail(buf);
        html = buf.toString();

        Matcher imageMatcher = Pattern.compile("<img[^<>]*src=\"([^\"]*)\"").matcher(html);
        while (imageMatcher.find()) {
            String url = imageMatcher.group(1);
            String fixedUrl = fixImagePath(url, workItem);
            byte[] imgBytes = loadImage(fixedUrl);
            if (imgBytes != null) {
                String hash = DigestUtils.md5DigestAsHex(imgBytes);
                String regex = Pattern.quote(url);
                Matcher matcher = Pattern.compile(regex).matcher(html);
                if (matcher.find()) {
                    html = matcher.replaceFirst(matcher.group() + "\" " + String.format("md5=\"%s\"", hash));
                }
            }
        }
        return html;
    }

    @VisibleForTesting
    byte[] loadImage(String resource) {
        return new ResourceProvider().getResourceAsBytes(resource);
    }

    @VisibleForTesting
    String fixImagePath(String url, WorkItem workItem) {
        if (url.startsWith(IWorkItem.ATTACHMENT_IMG_PREFIX)) {
            return url.replace(IWorkItem.ATTACHMENT_IMG_PREFIX, "/polarion/wi-attachment/" + workItem.getProjectId() + "/" + workItem.getId() + "/");
        }

        if (url.startsWith(IModule.ATTACHMENT_IMG_PREFIX)) {
            return url.replace(IModule.ATTACHMENT_IMG_PREFIX, "/polarion/module-attachment/" + workItem.getProjectId() + "/" + workItem.getModule().getModuleFolder() + "/" + workItem.getModule().getId() + "/");
        }

        return url;
    }
}
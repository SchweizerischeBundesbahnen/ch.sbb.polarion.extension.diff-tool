package ch.sbb.polarion.extension.diff_tool.service;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.ICommentBase;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IModuleComment;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.model.IPObjectList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Copies comment threads onto a copy of the object they were written on - a work item or a document.
 * <p>
 * A comment is an object of its own, so a copy gets comments of its own too, with new IDs. The mapping of the
 * old IDs to the new ones is what lets the markers which anchor a comment to a piece of text be pointed at the
 * copied comments (see {@code CommentUtils.copyCommentMarkers}).
 */
public class CommentsCopier {

    private static final Logger logger = Logger.getLogger(CommentsCopier.class);

    private final PolarionService polarionService;

    public CommentsCopier(@NotNull PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    /**
     * Copies the comments of a work item onto its copy.
     *
     * @return ID of a source comment mapped to the ID of its copy
     */
    public @NotNull Map<String, String> copyComments(@NotNull IWorkItem sourceWorkItem, @NotNull IWorkItem targetWorkItem) {
        return copyThreads(sourceWorkItem.getRootComments(true), targetWorkItem::createComment, null);
    }

    /**
     * Copies the comments of a document into another document.
     *
     * @param commentsToCopy which of the document's comment threads to copy
     * @param fallbackAuthor who a comment is attributed to when the author of the source comment cannot be set
     * @return ID of a source comment mapped to the ID of its copy
     */
    public @NotNull Map<String, String> copyComments(@NotNull IModule sourceModule, @NotNull IModule targetModule,
                                                     @NotNull Predicate<IModuleComment> commentsToCopy, @Nullable IUser fallbackAuthor) {
        // Iterated rather than streamed: this is a Polarion object list, and its own iterator is what reads it
        List<IModuleComment> rootComments = new ArrayList<>();
        IPObjectList<IModuleComment> sourceRootComments = sourceModule.getRootComments(true);
        if (sourceRootComments != null && !sourceRootComments.isEmpty()) {
            for (IModuleComment rootComment : sourceRootComments) {
                if (commentsToCopy.test(rootComment)) {
                    rootComments.add(rootComment);
                }
            }
        }
        return copyThreads(rootComments, targetModule::createComment, fallbackAuthor);
    }

    @VisibleForTesting
    <T extends ICommentBase<T>> @NotNull Map<String, String> copyThreads(@Nullable List<T> sourceRootComments,
                                                                        @NotNull Function<Text, T> rootCommentFactory,
                                                                        @Nullable IUser fallbackAuthor) {
        Map<String, String> idMapping = new LinkedHashMap<>();
        if (sourceRootComments == null || sourceRootComments.isEmpty()) {
            return idMapping;
        }
        for (T sourceRootComment : sourceRootComments) {
            T newRootComment = rootCommentFactory.apply(sourceRootComment.getText());
            copyMetadata(sourceRootComment, newRootComment, fallbackAuthor);
            newRootComment.save();
            idMapping.put(sourceRootComment.getId(), newRootComment.getId());
            copyChildComments(sourceRootComment, newRootComment, idMapping, fallbackAuthor);
            if (sourceRootComment.isResolvedComment()) {
                // The resolved flag can be set only on a root comment.
                // Set it only after all child comments have been created, which requires an additional save.
                newRootComment.setResolvedComment(true);
                newRootComment.save();
            }
        }
        return idMapping;
    }

    private <T extends ICommentBase<T>> void copyChildComments(@NotNull T sourceParent, @NotNull T targetParent,
                                                               @NotNull Map<String, String> idMapping, @Nullable IUser fallbackAuthor) {
        List<T> children = sourceParent.getChildComments();
        if (children == null) {
            return;
        }
        for (T sourceChild : children) {
            T newChild = targetParent.createChildComment(sourceChild.getText());
            copyMetadata(sourceChild, newChild, fallbackAuthor);
            newChild.save();
            idMapping.put(sourceChild.getId(), newChild.getId());
            copyChildComments(sourceChild, newChild, idMapping, fallbackAuthor);
        }
    }

    /**
     * Keeps a copied comment saying who wrote it and when. The author can be set only by a system user, and where
     * even that is refused the comment is attributed to the fallback author rather than being lost.
     */
    private <T extends ICommentBase<T>> void copyMetadata(@NotNull T source, @NotNull T target, @Nullable IUser fallbackAuthor) {
        target.setValue(ICommentBase.KEY_CREATED, source.getCreated());
        try {
            polarionService.getSecurityService().doAsSystemUser((PrivilegedAction<Void>) () -> {
                target.setValue(ICommentBase.KEY_AUTHOR, source.getAuthor());
                return null;
            });
        } catch (Exception e) {
            String authorName = source.getAuthor() != null ? source.getAuthor().getName() : "<null>";
            logger.warn("Could not assign source comment author [%s]: %s".formatted(authorName, e.getMessage()));
            if (fallbackAuthor != null) {
                try {
                    target.setValue(ICommentBase.KEY_AUTHOR, fallbackAuthor);
                    logger.warn("Fallback: [%s] assigned as a comment author".formatted(fallbackAuthor.getName()));
                } catch (Exception ex) {
                    logger.warn("Could not assign the fallback comment author either: " + ex.getMessage());
                }
            }
        }
    }
}

package ch.sbb.polarion.extension.diff_tool.service;

import com.polarion.alm.tracker.model.IWorkItem;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Marks a merge context which copies work items without linking them to their origins. There is no link role
 * to seek counterparts by, so counterparts are resolved by the mapping this context collects while copying.
 */
public interface IUnpairedCopyContext {

    /**
     * Maps the ID of an already copied source work item to its copy in the target project.
     */
    @NotNull
    Map<String, IWorkItem> getItemMapping();

    /**
     * Maps the ID of a comment of the work item which is being copied right now to the ID of its copy, so that the
     * markers anchoring a comment to a piece of its text can be pointed at the copied comment.
     */
    @NotNull
    Map<String, String> getCommentIdMapping();

}

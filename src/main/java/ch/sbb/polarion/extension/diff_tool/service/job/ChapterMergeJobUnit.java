package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.IProgressMonitor;
import com.polarion.platform.jobs.spi.AbstractJobUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * Runs a chapter merge as a Polarion job, so that merging a big chapter doesn't have to fit into one HTTP request.
 * <p>
 * The job runs as the user who scheduled it - Polarion spawns it under their subject - so it needs nothing of the
 * request which started it. It must not reach for that request either: the servlet container recycles the request
 * object as soon as the response is written, which is usually long before a merge of any size has finished.
 */
public class ChapterMergeJobUnit extends AbstractJobUnit {

    private final ChapterMergeParams params;
    private final DocumentsChapterMergeService documentsChapterMergeService;
    private final transient Consumer<MergeResult> resultConsumer;

    public ChapterMergeJobUnit(@NotNull ChapterMergeParams params, @NotNull DocumentsChapterMergeService documentsChapterMergeService,
                               @NotNull Consumer<MergeResult> resultConsumer, @NotNull IJobUnitFactory factory) {
        super(buildName(params), factory);
        this.params = params;
        this.documentsChapterMergeService = documentsChapterMergeService;
        this.resultConsumer = resultConsumer;
    }

    private static String buildName(@NotNull ChapterMergeParams params) {
        return "Merge chapter '%s' of '%s' into chapter '%s' of '%s'".formatted(
                params.getSourceChapterOutlineNumber(), params.getSourceDocument().getName(),
                params.getTargetChapterOutlineNumber(), params.getTargetDocument().getName());
    }

    @Override
    protected IJobStatus runInternal(IProgressMonitor monitor) {
        try {
            MergeResult mergeResult = documentsChapterMergeService.mergeChapter(params, message -> getLogger().info(message));
            resultConsumer.accept(mergeResult);
            if (mergeResult.getMergeReport() != null) {
                getLogger().info(mergeResult.getMergeReport().getLogs());
            }
            if (!mergeResult.isSuccess()) {
                return getStatusFailed(failureMessage(mergeResult), null);
            }
            return getStatusOK("Chapter '%s' merged into document '%s'".formatted(
                    params.getSourceChapterOutlineNumber(), params.getTargetDocument().getName()));
        } catch (CancellationException e) {
            getLogger().info("Chapter merge cancelled");
            return getStatusCancelled("Chapter merge cancelled by user");
        } catch (Exception e) {
            getLogger().error("Chapter merge failed", e);
            resultConsumer.accept(MergeResult.builder().success(false).build());
            return getStatusFailed("Chapter merge failed: " + e.getMessage(), e);
        } finally {
            monitor.done();
        }
    }

    @VisibleForTesting
    @NotNull
    String failureMessage(@NotNull MergeResult mergeResult) {
        if (mergeResult.isMergeNotAuthorized()) {
            return "Chapter merge is not authorized for the current user";
        }
        if (mergeResult.isTargetModuleHasStructuralChanges()) {
            return "Target document has been changed meanwhile, please reload it and try again";
        }
        return "Chapter merge failed, see the merge report for details";
    }
}

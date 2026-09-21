package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsMergeParams;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService;
import ch.sbb.polarion.extension.diff_tool.util.RequestContextUtil;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

@Singleton
@Secured
@Path("/api")
public class MergeApiController extends MergeInternalController {

    @SuppressWarnings("unused")
    public MergeApiController() {
        super();
    }

    public MergeApiController(@NotNull PolarionService polarionService, @NotNull ChapterMergeJobsService chapterMergeJobsService) {
        super(polarionService, chapterMergeJobsService);
    }

    @Override
    public MergeResult mergeDocuments(DocumentsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeDocuments(mergeParams));
    }

    @Override
    public MergeResult mergeDocumentsFields(DocumentsFieldsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeDocumentsFields(mergeParams));
    }

    @Override
    public MergeResult mergeDocumentsContent(DocumentsContentMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeDocumentsContent(mergeParams));
    }

    @Override
    public MergeResult mergeWorkItems(WorkItemsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeWorkItems(mergeParams));
    }

    /**
     * A chapter merge outlives the request which starts it, and so must the session it runs in: this call
     * authenticated itself, so it got a session of its own which the logout filter would end with this response.
     * The merge ends that session itself when it is over.
     * <p>
     * The flag has to be set before the merge is started, since that is where it is read, so a call which never
     * gets as far as a running merge - refused parameters, a document which cannot be read - gives the session
     * back: nothing else would end it, the merge which was to end it is not running.
     */
    @Override
    public Response mergeChapter(ChapterMergeParams mergeParams) {
        RequestContextUtil.keepSessionAlive();
        try {
            return polarionService.callPrivileged(() -> super.mergeChapter(mergeParams));
        } catch (Exception e) {
            RequestContextUtil.releaseSession();
            throw e;
        }
    }

    @Override
    public Response getAllChapterMergeJobs() {
        return polarionService.callPrivileged(super::getAllChapterMergeJobs);
    }

    @Override
    public Response getChapterMergeJob(String jobId) {
        return polarionService.callPrivileged(() -> super.getChapterMergeJob(jobId));
    }

    @Override
    public Response getChapterMergeJobResult(String jobId) {
        return polarionService.callPrivileged(() -> super.getChapterMergeJobResult(jobId));
    }
}

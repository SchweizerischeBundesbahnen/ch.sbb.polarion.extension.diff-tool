package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.osgi.framework.BundleContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(PlatformContextMockExtension.class)
class ExtensionBundleActivatorTest {

    @Test
    void testBundleActivator() {
        assertTrue(new ExtensionBundleActivator().getExtensions().keySet().containsAll(List.of("diff-tool", "copy-tool", "merge-tool")));
    }

    /**
     * Nothing else drops the results of finished chapter merges from memory, so the cleaner runs as long as this
     * bundle does. When the bundle stops, the merge threads stop with it.
     */
    @Test
    void testTheChapterMergeJobsRunWithTheBundle() {
        BundleContext context = mock(BundleContext.class);

        try (MockedStatic<ChapterMergeJobsService> jobsService = mockStatic(ChapterMergeJobsService.class)) {
            ExtensionBundleActivator activator = new ExtensionBundleActivator();

            activator.onStart(context);
            activator.stop(context);

            jobsService.verify(ChapterMergeJobsService::startCleaner);
            jobsService.verify(ChapterMergeJobsService::shutdown);
        }
    }

    /**
     * A cleaner which cannot be started must not keep the bundle from starting: everything else this extension
     * does works without it.
     */
    @Test
    void testABundleStartsEvenIfItsCleanerDoesNot() {
        BundleContext context = mock(BundleContext.class);

        try (MockedStatic<ChapterMergeJobsService> jobsService = mockStatic(ChapterMergeJobsService.class)) {
            jobsService.when(ChapterMergeJobsService::startCleaner).thenThrow(new IllegalStateException("boom"));

            new ExtensionBundleActivator().onStart(context);

            jobsService.verify(ChapterMergeJobsService::startCleaner);
        }
    }

}

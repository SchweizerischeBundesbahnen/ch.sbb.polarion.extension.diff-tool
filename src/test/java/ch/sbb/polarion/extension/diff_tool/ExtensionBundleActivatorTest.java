package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsCleaner;
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
     * bundle does.
     */
    @Test
    void testTheChapterMergeJobsCleanerRunsWithTheBundle() {
        BundleContext context = mock(BundleContext.class);

        try (MockedStatic<ChapterMergeJobsCleaner> cleaner = mockStatic(ChapterMergeJobsCleaner.class)) {
            ExtensionBundleActivator activator = new ExtensionBundleActivator();

            activator.onStart(context);
            activator.stop(context);

            cleaner.verify(ChapterMergeJobsCleaner::startCleaningJob);
            cleaner.verify(ChapterMergeJobsCleaner::stopCleaningJob);
        }
    }

    /**
     * A cleaner which cannot be started must not keep the bundle from starting: everything else this extension
     * does works without it.
     */
    @Test
    void testABundleStartsEvenIfItsCleanerDoesNot() {
        BundleContext context = mock(BundleContext.class);

        try (MockedStatic<ChapterMergeJobsCleaner> cleaner = mockStatic(ChapterMergeJobsCleaner.class)) {
            cleaner.when(ChapterMergeJobsCleaner::startCleaningJob).thenThrow(new IllegalStateException("boom"));

            new ExtensionBundleActivator().onStart(context);

            cleaner.verify(ChapterMergeJobsCleaner::startCleaningJob);
        }
    }

}

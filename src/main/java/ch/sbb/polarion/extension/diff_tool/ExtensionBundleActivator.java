package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsCleaner;
import ch.sbb.polarion.extension.generic.GenericBundleActivator;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.core.util.logging.Logger;
import org.osgi.framework.BundleContext;

import java.util.Map;

public class ExtensionBundleActivator extends GenericBundleActivator {

    private final Logger logger = Logger.getLogger(ExtensionBundleActivator.class);

    @Override
    protected Map<String, IFormExtension> getExtensions() {
        return Map.of(
                CopyToolFormExtension.DOCUMENTS_COPY_FORM_EXTENSION_ID, new CopyToolFormExtension(),
                DiffToolFormExtension.DOCUMENTS_COMPARISON_FORM_EXTENSION_ID, new DiffToolFormExtension(),
                MergeToolFormExtension.DOCUMENTS_MERGE_FORM_EXTENSION_ID, new MergeToolFormExtension()
        );
    }

    @Override
    protected void onStart(BundleContext context) {
        try {
            ChapterMergeJobsCleaner.startCleaningJob();
        } catch (Exception e) {
            logger.error("Error during starting of chapter merge jobs cleaner", e);
        }
    }

    @Override
    public void stop(BundleContext context) {
        ChapterMergeJobsCleaner.stopCleaningJob();
        super.stop(context);
    }

}

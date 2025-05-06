package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.GenericBundleActivator;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;

import java.util.Map;

public class ExtensionBundleActivator extends GenericBundleActivator {

    @Override
    protected Map<String, IFormExtension> getExtensions() {
        return Map.of(
                CopyToolFormExtension.DOCUMENTS_COPY_FORM_EXTENSION_ID, new CopyToolFormExtension(),
                DiffToolFormExtension.DOCUMENTS_COMPARISON_FORM_EXTENSION_ID, new DiffToolFormExtension()
        );
    }

}

package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.GenericModule;
import com.polarion.alm.ui.server.forms.extensions.FormExtensionContribution;

public class CopyToolModule extends GenericModule {
    public static final String DOCUMENTS_COPY_FORM_EXTENSION_ID = "copy-tool";

    @Override
    protected FormExtensionContribution getFormExtensionContribution() {
        return new FormExtensionContribution(CopyToolFormExtension.class, DOCUMENTS_COPY_FORM_EXTENSION_ID);
    }

}

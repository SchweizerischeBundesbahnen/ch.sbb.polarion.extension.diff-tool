package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.GenericModule;
import com.polarion.alm.ui.server.forms.extensions.FormExtensionContribution;

public class CopyToolModule extends GenericModule {

    @Override
    protected FormExtensionContribution getFormExtensionContribution() {
        return new FormExtensionContribution(CopyToolFormExtension.class, CopyToolFormExtension.DOCUMENTS_COPY_FORM_EXTENSION_ID);
    }

}

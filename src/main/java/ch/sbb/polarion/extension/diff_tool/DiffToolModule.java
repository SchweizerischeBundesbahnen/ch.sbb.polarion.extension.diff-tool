package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.GenericModule;
import com.polarion.alm.ui.server.forms.extensions.FormExtensionContribution;

public class DiffToolModule extends GenericModule {
    public static final String DOCUMENTS_COMPARISON_FORM_EXTENSION_ID = "diff-tool";

    @Override
    protected FormExtensionContribution getFormExtensionContribution() {
        return new FormExtensionContribution(DiffToolFormExtension.class, DOCUMENTS_COMPARISON_FORM_EXTENSION_ID);
    }

}

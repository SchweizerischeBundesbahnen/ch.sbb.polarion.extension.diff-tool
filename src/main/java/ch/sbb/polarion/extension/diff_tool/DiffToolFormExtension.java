package ch.sbb.polarion.extension.diff_tool;

import com.google.inject.Inject;

public class DiffToolFormExtension extends BaseFormExtension {

    @Inject
    public DiffToolFormExtension() {
        super(DiffToolModule.DOCUMENTS_COMPARISON_FORM_EXTENSION_ID, "Page Comparison");
    }
}

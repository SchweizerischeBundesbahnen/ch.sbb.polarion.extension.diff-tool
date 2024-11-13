package ch.sbb.polarion.extension.diff_tool;

import com.google.inject.Inject;

public class CopyToolFormExtension extends BaseFormExtension {

    @Inject
    public CopyToolFormExtension() {
        super(CopyToolModule.DOCUMENTS_COPY_FORM_EXTENSION_ID, "Documents Copy");
    }
}

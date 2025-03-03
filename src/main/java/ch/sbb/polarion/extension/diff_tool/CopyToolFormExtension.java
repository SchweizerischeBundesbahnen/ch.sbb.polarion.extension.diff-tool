package ch.sbb.polarion.extension.diff_tool;

import com.google.inject.Inject;

public class CopyToolFormExtension extends BaseFormExtension {
    public static final String DOCUMENTS_COPY_FORM_EXTENSION_ID = "copy-tool";

    @Inject
    public CopyToolFormExtension() {
        super(DOCUMENTS_COPY_FORM_EXTENSION_ID, "Documents Copy");
    }
}

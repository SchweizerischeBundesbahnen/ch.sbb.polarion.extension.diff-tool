package ch.sbb.polarion.extension.diff_tool.service;

/**
 * Marks a merge context which copies document attachments referenced by rich text into the target document.
 */
public interface ICopyModuleAttachmentsContext {

    boolean isCopyMissingDocumentAttachments();

}

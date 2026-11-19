package ch.sbb.polarion.extension.diff_tool.navigation;

/**
 * The topic which exports the diffs of several document pairs to PDF at once.
 */
public class DocumentsNode extends DiffToolTopicNode {
    public static final String NODE_ID = "documents-diff-to-pdf";

    public DocumentsNode() {
        super(NODE_ID, "Documents diff to PDF", "/polarion/ria/images/topicIconsSmall/documentsAndWiki.svg");
    }
}

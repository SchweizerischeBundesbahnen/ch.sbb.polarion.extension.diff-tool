package ch.sbb.polarion.extension.diff_tool.util;

import lombok.Getter;

@Getter
public enum ModificationType {
    ADDED("diff-html-added"),
    REMOVED("diff-html-removed"),
    MODIFIED("diff-html-changed"),
    NONE("unchanged");

    final String styleName;

    ModificationType(String styleName) {
        this.styleName = styleName;
    }
}

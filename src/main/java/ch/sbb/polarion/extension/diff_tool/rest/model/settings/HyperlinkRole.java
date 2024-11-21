package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HyperlinkRole {

    @EqualsAndHashCode.Include
    private String id;

    private String name;

    @EqualsAndHashCode.Include
    private String workItemTypeId;

    private String workItemTypeName;

    public String getCombinedId() {
        return workItemTypeId + "#" + id;
    }
}

package org.jetlinks.community.relation.service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.relation.entity.RelatedEntity;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class RelatedObjectInfo {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    public static RelatedObjectInfo ofRelated(RelatedEntity related) {
        return new RelatedObjectInfo(related.getRelatedId(), related.getRelatedName());
    }
}

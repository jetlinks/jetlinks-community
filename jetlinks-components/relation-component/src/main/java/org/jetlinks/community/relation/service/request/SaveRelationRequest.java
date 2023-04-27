package org.jetlinks.community.relation.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.relation.service.RelatedObjectInfo;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SaveRelationRequest {

    @Schema(description = "关系类型")
    private String relatedType;

    @Schema(description = "关系ID")
    private String relation;

    @Schema(description = "关系目标列里表")
    private List<RelatedObjectInfo> related;

    @Schema(description = "说明")
    private String description;
}

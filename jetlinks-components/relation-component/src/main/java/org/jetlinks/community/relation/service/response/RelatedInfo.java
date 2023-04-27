package org.jetlinks.community.relation.service.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.relation.service.RelatedObjectInfo;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RelatedInfo {
    private String objectId;
    private String relation;
    private String relationName;
    private Map<String,Object> relationExpands;

    private String relatedType;
    private List<RelatedObjectInfo> related;

}

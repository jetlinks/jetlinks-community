package org.jetlinks.community.auth.relation;

import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.things.relation.ObjectProperty;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.jetlinks.community.auth.entity.PositionEntity;
import org.jetlinks.community.auth.service.PositionService;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.impl.ObjectData;
import org.jetlinks.community.relation.impl.SimpleObjectType;
import org.jetlinks.community.relation.impl.SimpleRelation;
import org.jetlinks.community.relation.impl.property.PropertyOperationStrategy;
import org.jetlinks.community.relation.utils.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;

@AllArgsConstructor
@Component
public class PositionRelationObjectProvider implements RelationObjectProvider {

    private final PositionService service;

    public static final ObjectType TYPE =
        new SimpleObjectType(RelationObjectProvider.TYPE_POSITION, "职位", "职位")
            .withRelation(RelationObjectProvider.TYPE_USER,
                          Arrays.asList(
                              SimpleRelation.of(
                                  "parent",
                                  "上级职位",
                                  "下级职位",
                                  false,
                                  Collections.emptyMap()
                              ),
                              SimpleRelation.of(
                                  "org",
                                  "所属组织",
                                  "组织职位",
                                  false,
                                  Collections.emptyMap()
                              )
                          ))
            .withExpand(ObjectUtils.queryPermissionId.getKey(), "organization");

    @Override
    public String getTypeId() {
        return RelationObjectProvider.TYPE_POSITION;
    }

    @Override
    public Mono<ObjectType> getType() {
        return Mono.just(TYPE);
    }

    @Override
    public PropertyOperation properties(String id) {
        return PropertyOperationStrategy
            .simple(
                Mono.defer(() -> Mono.justOrEmpty(service.getCached(id))),
                opt -> {
                    opt.addMapper(ObjectProperty.name, PositionEntity::getName);
                    opt.addMapper(ObjectProperty.description, PositionEntity::getDescription);
                    //上级职位
                    opt.addMapper("parentId", PositionEntity::getParentId);
                    //所在组织
                    opt.addMapper("orgId", PositionEntity::getOrgId);
                    //更多属性支持
                }
            );
    }


    @Override
    public Flux<PropertyMetadata> metadata() {
        return Flux.just(
            SimplePropertyMetadata.of("id", "id", StringType.GLOBAL),
            SimplePropertyMetadata.of("name", "组织名称", StringType.GLOBAL),
            SimplePropertyMetadata.of("description", "说明", StringType.GLOBAL),
            SimplePropertyMetadata.of("code", "编码", StringType.GLOBAL),
            SimplePropertyMetadata.of("parentId", "上级职位", StringType.GLOBAL),
            SimplePropertyMetadata.of("orgId", "所属组织", StringType.GLOBAL)
        );
    }

    @Override
    public Flux<ObjectData> doQuery(QueryParamEntity query) {
        return service
            .query(ObjectUtils.refactorParam(query.noPaging()))
            .map(this::toObjectData);
    }

    @Override
    public Mono<PagerResult<ObjectData>> doQueryPage(QueryParamEntity query) {
        return service
            .queryPager(ObjectUtils.refactorParam(query), this::toObjectData);
    }

    private ObjectData toObjectData(PositionEntity entity) {
        return FastBeanCopier.copy(entity, new ObjectData(), FastBeanCopier.include(
            "id", "name", "description", "code", "parentId"));
    }


}

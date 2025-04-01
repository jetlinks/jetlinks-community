package org.jetlinks.community.auth.relation;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultReactiveUserService;
import org.jetlinks.community.relation.impl.ObjectData;
import org.jetlinks.community.relation.utils.ObjectUtils;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.jetlinks.community.auth.entity.ThirdPartyUserBindEntity;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.service.UserDetailService;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.jetlinks.community.relation.RelationConstants;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.impl.SimpleObjectType;
import org.jetlinks.community.relation.impl.SimpleRelation;
import org.jetlinks.community.relation.impl.property.PropertyOperationStrategy;
import org.jetlinks.community.relation.impl.property.SimplePropertyOperation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


@AllArgsConstructor
@Component
public class UserRelationObjectProvider implements RelationObjectProvider {

    private final UserDetailService detailService;

    private final DefaultReactiveUserService service;

    private final ReactiveRepository<ThirdPartyUserBindEntity, String> repository;

    public static final List<Consumer<SimplePropertyOperation<UserDetail>>>
        customizer = new CopyOnWriteArrayList<>();

    @Override
    public String getTypeId() {
        return RelationObjectProvider.TYPE_USER;
    }

    @Override
    public Mono<ObjectType> getType() {
        return Mono.just(
            new SimpleObjectType(RelationObjectProvider.TYPE_USER, "用户", "系统用户")
                .withExpand(ObjectUtils.queryPermissionId.getKey(), "user")
                .withRelation(
                    PositionRelationObjectProvider.TYPE,
                    Collections.singletonList(
                        SimpleRelation.of(
                            OrgDimensionType.position.getId(),
                            "所在岗位",
                            "人员",
                            false,
                            Collections.emptyMap()
                        )
                    )
                ));
    }

    @Override
    public PropertyOperation properties(String id) {
        return PropertyOperationStrategy
            .composite(
                //用户基本信息
                PropertyOperationStrategy
                    .simple(
                        Mono.defer(() -> detailService.findUserDetail(id)).cache(),
                        strategy -> {
                            // admin@user.id
                            strategy.addMapper(RelationConstants.UserProperty.id, UserDetail::getId);
                            // admin@user.name
                            strategy.addMapper(RelationConstants.UserProperty.name, UserDetail::getName);
                            // admin@user.username
                            strategy.addMapper(RelationConstants.UserProperty.username, UserDetail::getUsername);
                            // admin@user.email
                            strategy.addMapper(RelationConstants.UserProperty.email, UserDetail::getEmail);
                            // admin@user.telephone
                            strategy.addMapper(RelationConstants.UserProperty.telephone, UserDetail::getTelephone);
                            // admin@user.departments
                            strategy.addMapper(RelationConstants.UserProperty.departments, UserDetail::getOrgIdList);
                            // admin@user.organizations
                            strategy.addMapper(RelationConstants.UserProperty.organizations, UserDetail::getOrgIdList);
                            // admin@user.roles
                            strategy.addMapper(RelationConstants.UserProperty.roles, UserDetail::getRoleIdList);
                            // admin@user.positions
                            strategy.addMapper(RelationConstants.UserProperty.positions, UserDetail::getPositionIdList);
                            // admin@user.parentPositions
                            strategy.addMapper(RelationConstants.UserProperty.parentPositions, UserDetail::getParentPositionIdList);

                            // admin@user.*
                            strategy.addMapper(RelationConstants.UserProperty.all, detail -> detail);
                            //自定义拓展
                            for (Consumer<SimplePropertyOperation<UserDetail>> consumer : customizer) {
                                consumer.accept(strategy);
                            }
                        }
                    ),
                //第三方绑定信息
                PropertyOperationStrategy
                    .detect(strategy -> {
                        // admin@user.third.dingtalk.provider
                        strategy.addOperation(
                            "third",
                            key -> {
                                String[] typeAndProvider = key.split("[.]", 2);
                                return repository
                                    .createQuery()
                                    .where(ThirdPartyUserBindEntity::getUserId, id)
                                    .and(ThirdPartyUserBindEntity::getType, typeAndProvider[0])
                                    .and(ThirdPartyUserBindEntity::getProvider,
                                         typeAndProvider.length == 2 ? typeAndProvider[1] : null)
                                    .fetch()
                                    .map(ThirdPartyUserBindEntity::getThirdPartyUserId)
                                    .singleOrEmpty()
                                    .cast(Object.class);
                            }
                        );

                    })
            );
    }

    @Override
    public Flux<PropertyMetadata> metadata() {
        return Flux.just(
            SimplePropertyMetadata.of("id", "id", StringType.GLOBAL),
            SimplePropertyMetadata.of("name", "姓名", StringType.GLOBAL),
            SimplePropertyMetadata.of("state", "用户状态", BooleanType.GLOBAL
                .falseValue("0")
                .falseText("禁用")
                .trueValue("1")
                .trueText("正常")),
            SimplePropertyMetadata.of("describe", "说明", StringType.GLOBAL),
            SimplePropertyMetadata.of("username", "用户名", StringType.GLOBAL)
        );
    }

    @Override
    public Flux<ObjectData> doQuery(QueryParamEntity query) {
        return service
            .query(ObjectUtils.refactorParam(query.noPaging()))
            .map(this::toObjectData)
            .switchIfEmpty(Flux.defer(() -> RelationObjectProvider.super.doQuery(query)));
    }

    @Override
    public Flux<ObjectData> getRelated(String type,
                                       String relation,
                                       boolean reverse,
                                       Collection<String> objectId,
                                       Collection<String> targetId) {

        if (OrgDimensionType.position.isSameType(type) &&
            OrgDimensionType.position.isSameType(relation)) {
            //获取用户所在岗位
            return reverse
                ? getPositionUsers(relation, objectId, targetId)
                : getUserPositions(relation, objectId, targetId);
        }
        return RelationObjectProvider.super.getRelated(type, relation, reverse, objectId, targetId);
    }

    private Flux<ObjectData> getPositionUsers(String type,
                                              Collection<String> userId,
                                              Collection<String> positionId) {
        if (CollectionUtils.isEmpty(userId)) {
            return Flux.empty();
        }
        return Flux
            .fromIterable(userId)
            .flatMap(ReactiveAuthenticationHolder::get)
            .filter(auth -> auth.hasDimension(OrgDimensionType.position.getId(), positionId))
            .map(auth -> {
                ObjectData data = new ObjectData();
                data.put("id", auth.getUser().getId());
                data.put("name", auth.getUser().getName());
                data.put("username", auth.getUser().getUsername());
                return data;
            });
    }

    private Flux<ObjectData> getUserPositions(String type,
                                              Collection<String> userId,
                                              Collection<String> positionId) {
        if (CollectionUtils.isEmpty(userId)) {
            return Flux.empty();
        }
        return Flux
            .fromIterable(userId)
            .flatMap(ReactiveAuthenticationHolder::get)
            .flatMapIterable(auth -> Collections2.transform(
                Collections2.filter(
                    auth.getDimensions(type),
                    dim -> CollectionUtils.isEmpty(positionId) || positionId.contains(dim.getId())),
                dim -> {
                    ObjectData data = new ObjectData();
                    data.put("id", dim.getId());
                    data.put("name", dim.getName());
                    data.putAll(dim.getOptions());
                    return data;
                }
            ));
    }

    @Override
    public Mono<PagerResult<ObjectData>> doQueryPage(QueryParamEntity query) {
        return service
            .queryPager(ObjectUtils.refactorParam(query),
                        this::toObjectData)
            .switchIfEmpty(Mono.defer(() -> RelationObjectProvider.super.doQueryPage(query)));
    }

    private ObjectData toObjectData(UserEntity entity) {
        return FastBeanCopier.copy(entity, new ObjectData(), FastBeanCopier.include(
            "id", "name", "state", "describe", "username"));
    }
}

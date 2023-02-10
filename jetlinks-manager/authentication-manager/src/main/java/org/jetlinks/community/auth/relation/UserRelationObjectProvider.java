package org.jetlinks.community.auth.relation;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.jetlinks.community.auth.entity.ThirdPartyUserBindEntity;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.service.UserDetailService;
import org.jetlinks.community.relation.RelationConstants;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.impl.SimpleObjectType;
import org.jetlinks.community.relation.impl.property.PropertyOperationStrategy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@AllArgsConstructor
@Component
public class UserRelationObjectProvider implements RelationObjectProvider {

    private final UserDetailService detailService;

    private final ReactiveRepository<ThirdPartyUserBindEntity, String> repository;

    @Override
    public String getTypeId() {
        return RelationObjectProvider.TYPE_USER;
    }

    @Override
    public Mono<ObjectType> getType() {
        return Mono.just(new SimpleObjectType(getTypeId(), "用户", "系统用户"));
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
                            // admin@user.roles
                            strategy.addMapper(RelationConstants.UserProperty.roles, UserDetail::getRoleIdList);
                            // admin@user.*
                            strategy.addMapper(RelationConstants.UserProperty.all, detail -> detail);
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
}

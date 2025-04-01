package org.jetlinks.community.relation;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.relation.impl.ObjectData;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.PropertyOperation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

public interface RelationObjectProvider {

    String TYPE_USER = "user";

    String TYPE_ORG = "org";

    String TYPE_ROLE = "role";

    String TYPE_DEVICE = "device";

    String TYPE_PRODUCT = "product";

    String TYPE_POSITION = "position";

    String getTypeId();

    Mono<ObjectType> getType();

    /**
     * 获取对象的操作描述
     */
    PropertyOperation properties(String id);

    /**
     * 获取对象数据的属性描述
     */
    default Flux<PropertyMetadata> metadata() {
        return Flux.empty();
    }


    /**
     * 查询对象数据
     */
    default Flux<ObjectData> doQuery(QueryParamEntity query) {
        return Flux.empty();
    }


    /**
     * 分页查询对象数据
     */
    default Mono<PagerResult<ObjectData>> doQueryPage(QueryParamEntity query) {
        return Mono.empty();
    }


    /**
     * 根据固定的关系信息查询数据,如: 获取设备(符合objectParam)的创建人(且创建人type in targetId).
     *
     * @param type        对象类型
     * @param relation    关系
     * @param reverse     反转查询（如：获取创建人type in targetId的设备(符合objectParam)数据）
     * @param objectParam 关系对象符合条件
     * @param targetId    目标关系对象ID，为空则为全部
     * @return 关系对象
     */
    default Flux<ObjectData> doQueryByRelated(String type,
                                              String relation,
                                              boolean reverse,
                                              QueryParamEntity objectParam,
                                              Collection<String> targetId) {
        return Flux.empty();
    }

    /**
     * 根据固定的关系信息分页查询数据,如: 获取设备(符合objectParam)的创建人(且创建人type in targetId).
     *
     * @param type        对象类型
     * @param relation    关系
     * @param reverse     反转查询（如：获取创建人type in targetId的设备(符合objectParam)数据）
     * @param objectParam 关系对象符合条件
     * @param targetId    目标关系对象ID，为空则为全部
     * @return 关系对象
     */
    default Mono<PagerResult<ObjectData>> doQueryByRelatedPage(String type,
                                                               String relation,
                                                               boolean reverse,
                                                               QueryParamEntity objectParam,
                                                               Collection<String> targetId) {
        return Mono.empty();
    }


    /**
     * 获取固定的关系信息,如: 获取设备的创建人(且创建人 in targetId).
     *
     * @param type     对象类型
     * @param relation 关系
     * @param targetId 目标关系对象ID，为空则为全部
     * @return 关系对象
     */
    @Deprecated
    default Flux<ObjectData> getRelated(String type,
                                        String relation,
                                        Collection<String> targetId) {
        return getRelated(type, relation, false, Collections.emptyList(), targetId);
    }

    /**
     * 获取固定的关系信息,如: 获取设备(in objectId)的创建人(且创建人type in targetId).
     *
     * @param type     对象类型
     * @param relation 关系
     * @param reverse  反转查询（如：获取创建人type in targetId的设备(in objectId)数据）
     * @param objectId 关系对象ID，为空则为全部
     * @param targetId 目标关系对象ID，为空则为全部
     * @return 关系对象
     */
    default Flux<ObjectData> getRelated(String type,
                                        String relation,
                                        boolean reverse,
                                        Collection<String> objectId,
                                        Collection<String> targetId) {
        return doQueryByRelated(type,
                                relation,
                                reverse,
                                QueryParamEntity
                                    .newQuery()
                                    .includes("id")
                                    .when(CollectionUtils.isNotEmpty(objectId), q -> q.in("id", objectId))
                                    .getParam(),
                                targetId);
    }


}

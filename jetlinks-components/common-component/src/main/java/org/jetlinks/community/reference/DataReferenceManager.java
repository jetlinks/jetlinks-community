package org.jetlinks.community.reference;

import org.apache.commons.collections4.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 数据引用管理器,用于获取、判断指定的数据是否被其他地方所引用.
 * <p>
 * 可以通过实现接口: {@link DataReferenceProvider}来定义数据引用者.
 * <p>
 * 使用场景: 在对一些数据进行删除操作时，可以通过判断引用关系来阻止删除.
 * 比如: 删除网络组件时,如果网络组件已经被其他地方使用,则不能删除.
 *
 * @author zhouhao
 * @see DataReferenceProvider
 * @since 2.0
 */
public interface DataReferenceManager {

    //数据类型: 设备接入网关
    String TYPE_DEVICE_GATEWAY = "device-gateway";
    //数据类型: 网络组件
    String TYPE_NETWORK = "network";
    //数据类型：关系配置
    String TYPE_RELATION = "relation";
    //数据类型：消息协议
    String TYPE_PROTOCOL = "protocol";

    /**
     * 判断指定数据类型的数据是否已经被其他地方所引用
     *
     * @param dataType 数据类型
     * @param dataId   数据ID
     * @return 是否已经被引用
     */
    Mono<Boolean> isReferenced(String dataType, String dataId);

    /**
     * 获取指定类型数据的引用关系
     *
     * @param dataType 数据类型
     * @param dataId   数据ID
     * @return 引用信息
     */
    Flux<DataReferenceInfo> getReferences(String dataType, String dataId);

    /**
     * 获取指定类型数据的全部引用关系
     *
     * @param dataType 数据类型
     * @return 引用信息
     */
    Flux<DataReferenceInfo> getReferences(String dataType);


    /**
     * 断言数据没有被引用,如果存在引用，则抛出异常: {@link DataReferencedException}
     *
     * @param dataType 数据类型
     * @param dataId   数据ID
     * @return void
     */
    default Mono<Void> assertNotReferenced(String dataType, String dataId) {
        return this
            .getReferences(dataType, dataId)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> Mono.error(new DataReferencedException(dataType, dataId, list)));
    }

}

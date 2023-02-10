package org.jetlinks.community.device.service.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.community.device.entity.DeviceLatestData;
import org.jetlinks.community.doc.QueryConditionOnly;
import org.jetlinks.community.timeseries.query.AggregationColumn;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 设备最新数据服务,用于保存设备最新的相关数据到关系型数据库中，可以使用动态条件进行查询相关数据
 *
 * @author zhouhao
 * @since 1.5.0
 */
public interface DeviceLatestDataService {

    /**
     * 根据物模型更新产品表结构信息
     *
     * @param productId 产品ID
     * @param metadata  物模型
     * @return void
     */
    Mono<Void> upgradeMetadata(String productId, DeviceMetadata metadata);

    /**
     * 重新加载物模型信息
     *
     * @param productId 产品ID
     * @param metadata  物模型
     * @return void
     */
    Mono<Void> reloadMetadata(String productId, DeviceMetadata metadata);

    /**
     * 保存消息数据
     *
     * @param message 设备消息
     */
    void save(DeviceMessage message);

    /**
     * 根据产品ID 查询最新的数据
     *
     * @param productId 产品ID
     * @param param     查询参数
     * @return 数据列表
     */
    Flux<DeviceLatestData> query(String productId, QueryParamEntity param);

    /**
     * 查询设备最新属性数据
     *
     * @param productId 产品ID
     * @param deviceId  设备ID
     * @return 属性数据
     */
    Mono<DeviceLatestData> queryDeviceData(String productId, String deviceId);

    /**
     * 根据产品ID查询数量
     *
     * @param productId 产品ID
     * @param param     参数
     * @return 查询数量
     */
    Mono<Integer> count(String productId, QueryParamEntity param);

    /**
     * 根据产品ID分页查询数据
     *
     * @param productId 产品ID
     * @param param     查询条件参数
     * @return 分页结果数据
     */
    default Mono<PagerResult<DeviceLatestData>> queryPager(String productId, QueryParamEntity param) {
        return Mono
            .zip(
                query(productId, param).collectList(),
                count(productId, param),
                (data, total) -> PagerResult.of(total, data, param)
            )
            .defaultIfEmpty(PagerResult.empty());
    }

    /**
     * 根据产品ID聚合查询数据
     *
     * @param productId   产品ID
     * @param columns     聚合列
     * @param paramEntity 查询条件参数
     * @return 聚合结果
     */
    Mono<Map<String, Object>> aggregation(String productId,
                                          List<AggregationColumn> columns,
                                          QueryParamEntity paramEntity);

    /**
     * 聚合查询多个产品下设备最新的数据
     *
     * @param param 参数
     * @param merge 是否将所有数据合并在一起
     * @return 查询结果
     */
    Flux<Map<String, Object>> aggregation(Flux<QueryProductLatestDataRequest> param,
                                          boolean merge);


    @Getter
    @Setter
    class QueryProductLatestDataRequest extends QueryLatestDataRequest {
        @NotBlank
        @Schema(defaultValue = "产品ID")
        private String productId;
    }

    @Getter
    @Setter
    class QueryLatestDataRequest {
        @NotNull
        private List<AggregationColumn> columns;

        @Schema(implementation = QueryConditionOnly.class)
        private QueryParamEntity query = QueryParamEntity.of();
    }
}

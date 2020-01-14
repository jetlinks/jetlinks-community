package org.jetlinks.community.standalone.init;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jetlinks.community.device.events.handler.DeviceIndexProvider;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Order(1)
@Slf4j
public class ElasticSearchIndexInitCenter implements CommandLineRunner {

    private final IndexOperationService indexOperationService;

    @Autowired
    public ElasticSearchIndexInitCenter(IndexOperationService indexOperationService) {
        this.indexOperationService = indexOperationService;
    }

    private static Map<String, CreateIndexRequest> store = new HashMap<>();

    static {


        /**
         * @see org.jetlinks.pro.device.logger.DeviceOperationLog
         */
        store.put(DeviceIndexProvider.DEVICE_OPERATION.getStandardIndex(), CreateIndex.createInstance()
                .addIndex(DeviceIndexProvider.DEVICE_OPERATION.getStandardIndex())
                .createMapping()
                .addFieldName("updateTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
                .addFieldName("createTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
                .addFieldName("deviceId").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("orgId").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("type").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("productId").addFieldType(FieldType.KEYWORD).commit()
                .end()
                .createIndexRequest());

        /**
         * @see org.jetlinks.pro.device.entity.DevicePropertiesEntity
         */
        store.put(DeviceIndexProvider.DEVICE_PROPERTIES.getStandardIndex(), CreateIndex.createInstance()
                .addIndex(DeviceIndexProvider.DEVICE_PROPERTIES.getStandardIndex())
                .createMapping()
                .addFieldName("createTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
                .addFieldName("updateTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
                .addFieldName("deviceId").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("property").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("propertyName").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("formatValue").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("value").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("orgId").addFieldType(FieldType.KEYWORD).commit()
                .addFieldName("productId").addFieldType(FieldType.KEYWORD).commit()
                .end()
                .createIndexRequest());
    }


    @Override
    public void run(String... args) throws Exception {
        store.forEach((key, value) -> {
            indexOperationService.init(value).subscribe(bool -> {
                if (bool) {
                    log.info("初始化 index:{},成功", key);
                }
            });
        });
    }
}

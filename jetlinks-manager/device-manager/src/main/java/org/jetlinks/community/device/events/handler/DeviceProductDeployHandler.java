package org.jetlinks.community.device.events.handler;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.index.mapping.MappingFactory;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Slf4j
@Order(1)
public class DeviceProductDeployHandler implements CommandLineRunner {


    private final IndexOperationService indexOperationService;

    private final LocalDeviceProductService productService;

    private DeviceMetadataCodec codec = new JetLinksDeviceMetadataCodec();

    @Autowired
    public DeviceProductDeployHandler(IndexOperationService indexOperationService,
                                      LocalDeviceProductService productService) {
        this.indexOperationService = indexOperationService;
        this.productService = productService;
    }

    @EventListener
    public void handlerEvent(DeviceProductDeployEvent event) {
        initDeviceEventIndex(event.getMetadata(), event.getId());
        initDevicePropertiesIndex(event.getId());
    }

    private void initDeviceEventIndex(String metadata, String productId) {
        codec.decode(metadata)
            .flatMap(meta -> Mono.justOrEmpty(meta.getEvents()))
            .flatMapIterable(Function.identity())
            .subscribe(eventMetadata -> {
                CreateIndex createIndex = CreateIndex.createInstance()
                    .addIndex(DeviceEventIndex.getDeviceEventIndex(productId, eventMetadata.getId()).getStandardIndex());
                MappingFactory mapping = ValueTypeRecordToESHandler.handler(
                    addEventDefaultMapping(MappingFactory.createInstance(createIndex))
                    , eventMetadata.getType(),
                    eventMetadata.getId());
                init(mapping.end().createIndexRequest());
            });
    }

    private void initDevicePropertiesIndex(String productId) {
        CreateIndex createIndex = CreateIndex.createInstance()
            .addIndex(DeviceEventIndex.getDevicePropertiesIndex(productId).getStandardIndex());
        init(addPropertyDefaultMapping(MappingFactory.createInstance(createIndex)).end().createIndexRequest());
    }

    private void init(CreateIndexRequest request) {
        indexOperationService.init(request).subscribe(bool -> {
            if (bool) {
                log.info("初始化 设备事件index:{},成功", request.index());
            }
        });
    }

    private MappingFactory addEventDefaultMapping(MappingFactory mappingFactory) {
        return mappingFactory
            .addFieldName("deviceId")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("productId")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("orgId")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("createTime")
            .addFieldType(FieldType.DATE)
            .addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time)
            .commit();
    }

    private MappingFactory addPropertyDefaultMapping(MappingFactory mappingFactory) {
        return mappingFactory
            .addFieldName("deviceId")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("productId")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("propertyName")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("property")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("orgId")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("value")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("objectValue")
            .addFieldType(FieldType.OBJECT)
            .commit()
            .addFieldName("numberValue")
            .addFieldType(FieldType.DOUBLE)
            .commit()
            .addFieldName("timeValue")
            .addFieldType(FieldType.DATE)
            .commit()
            .addFieldName("stringValue")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .commit()
            .addFieldName("formatValue")
            .addFieldType(FieldType.KEYWORD)
            .commit()
            .addFieldName("timestamp")
            .addFieldType(FieldType.DATE)
            .addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time)
            .commit();
    }


    @Override
    public void run(String... args) throws Exception {
        productService.createQuery()
            .fetch()
            .filter(productService -> productService.getState() == (byte) 1)
            .subscribe(deviceProductEntity -> {
                initDeviceEventIndex(deviceProductEntity.getMetadata(), deviceProductEntity.getId());
                initDevicePropertiesIndex(deviceProductEntity.getId());
            });
    }
}

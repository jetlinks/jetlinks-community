package org.jetlinks.community.elastic.search.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.index.mapping.MappingFactory;
import org.jetlinks.community.elastic.search.manager.StandardsIndexManager;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.jetlinks.community.elastic.search.service.IndexTemplateOperationService;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class ESTimeSeriesManager extends ESAbstractTimeSeriesManager {

    private final TimeSeriesServiceRegisterCenter timeSeriesServiceRegisterCenter;

    private final IndexOperationService indexOperationService;

    private final IndexTemplateOperationService indexTemplateOperationService;

    private final StandardsIndexManager standardsIndexManager;

    public ESTimeSeriesManager(TimeSeriesServiceRegisterCenter timeSeriesServiceRegisterCenter,
                               IndexOperationService indexOperationService,
                               StandardsIndexManager standardsIndexManager,
                               IndexTemplateOperationService indexTemplateOperationService) {
        this.timeSeriesServiceRegisterCenter = timeSeriesServiceRegisterCenter;
        this.indexOperationService = indexOperationService;
        this.standardsIndexManager = standardsIndexManager;
        this.indexTemplateOperationService = indexTemplateOperationService;
    }

    @Override
    public StandardsIndexManager getStandardsIndexManager() {
        return this.standardsIndexManager;
    }

    @Override
    public IndexOperationService getIndexOperationService() {
        return indexOperationService;
    }

    @Override
    public IndexTemplateOperationService getIndexTemplateOperationService() {
        return indexTemplateOperationService;
    }

    @Override
    public TimeSeriesService getService(TimeSeriesMetric metric) {
        return timeSeriesServiceRegisterCenter.getTimeSeriesService(getLocalTimeSeriesMetric(metric));
    }

    @Override
    public Mono<Void> registerMetadata(TimeSeriesMetadata metadata) {
        LocalTimeSeriesMetric localTimeSeriesMetric = getLocalTimeSeriesMetric(metadata.getMetric());
        return registerIndexTemplate(localTimeSeriesMetric, metadata.getProperties())
            .doOnError(e -> log.error("初始化模板:{}失败", localTimeSeriesMetric.getTemplateName(), e))
            .then();
    }

    private Mono<Boolean> registerIndexTemplate(LocalTimeSeriesMetric localTimeSeriesMetric, List<PropertyMetadata> properties) {
        return Mono.<PutIndexTemplateRequest>create(sink -> {
            MappingFactory factory = CreateIndex.createInstance().createMapping();
            properties
                .forEach(propertyMetadata -> addField(propertyMetadata, factory));
            addDefaultProperty(factory);

            PutIndexTemplateRequest request = new PutIndexTemplateRequest(localTimeSeriesMetric.getTemplateName());
            request.alias(localTimeSeriesMetric.getAlias());
            request.mapping(Collections.singletonMap("properties", factory.end().getMapping()));
            request.patterns(localTimeSeriesMetric.getIndexTemplatePatterns());
            sink.success(request);
        })
            .flatMap(indexTemplateOperationService::putTemplate)
            .doOnError(e -> log.error("初始化template:{}失败", localTimeSeriesMetric.getTemplateName(), e));
    }


    private void addDefaultProperty(MappingFactory factory) {
        factory
            .addFieldName("timestamp")
            .addFieldType(FieldType.DATE)
            .addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time)
            .commit();
    }

    private void addField(PropertyMetadata property, MappingFactory factory) {
        // TODO: 2020/2/4  对object 类型的支持
        DataType type = property.getValueType();
        if (type.getType().equalsIgnoreCase("date")) {
            DateTimeType dateTimeType = (DateTimeType) type;
            factory
                .addFieldName(property.getId())
                .addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time)
                //.addFieldDateFormat(dateTimeType.getFormat())
                .addFieldType(FieldType.DATE)
                .commit();
        } else {
            factory
                .addFieldName(property.getId())
                .addFieldType(FieldType.ofJava(type.getType()))
                .commit();
        }
    }
}

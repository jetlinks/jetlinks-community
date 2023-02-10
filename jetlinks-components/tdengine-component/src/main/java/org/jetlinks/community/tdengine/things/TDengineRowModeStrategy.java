package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.metadata.MetadataFeature;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.DDLOperations;
import org.jetlinks.community.things.data.operations.QueryOperations;
import org.jetlinks.community.things.data.operations.SaveOperations;
import reactor.core.publisher.Flux;

public class TDengineRowModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final TDengineThingDataHelper helper;

    public TDengineRowModeStrategy(ThingsRegistry registry, TDengineThingDataHelper helper) {
        this.registry = registry;
        this.helper = helper;
    }


    @Override
    public String getId() {
        return "tdengine-row";
    }

    @Override
    public String getName() {
        return "TDengine-行式存储";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new TDengineRowModeSaveOperations(
            registry,
            context.getMetricBuilder(),
            context.getSettings(),
            helper);
    }

    @Override
    protected DDLOperations createForDDL(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TDengineRowModeDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            context.getMetricBuilder(),
            helper);
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TDengineRowModeQueryOperations(
            thingType,
            templateId,
            thingId,
            context.getMetricBuilder(),
            context.getSettings(),
            registry,
            helper);
    }

    @Override
    public Flux<Feature> getFeatures() {
        //事件不支持新增以及修改
        return Flux.just(MetadataFeature.eventNotInsertable,
                         MetadataFeature.eventNotModifiable
        );
    }

    @Override
    public int getOrder() {
        return 10210;
    }

}

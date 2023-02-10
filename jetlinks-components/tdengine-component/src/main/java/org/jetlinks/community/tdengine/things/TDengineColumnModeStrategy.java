package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.DDLOperations;
import org.jetlinks.community.things.data.operations.QueryOperations;
import org.jetlinks.community.things.data.operations.SaveOperations;

public class TDengineColumnModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final TDengineThingDataHelper helper;

    public TDengineColumnModeStrategy(ThingsRegistry registry, TDengineThingDataHelper helper) {
        this.registry = registry;
        this.helper = helper;
    }

    @Override
    public String getId() {
        return "tdengine-column";
    }

    @Override
    public String getName() {
        return "TDEngine-列式存储";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new TDengineColumnModeSaveOperations(
            registry,
            context.getMetricBuilder(),
            context.getSettings(),
            helper);
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TDengineColumnModeQueryOperations(
            thingType,
            templateId,
            thingId,
            context.getMetricBuilder(),
            context.getSettings(),
            registry,
            helper);
    }

    @Override
    protected DDLOperations createForDDL(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TDengineColumnModeDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            context.getMetricBuilder(),
            helper);
    }

    @Override
    public int getOrder() {
        return 10220;
    }
}

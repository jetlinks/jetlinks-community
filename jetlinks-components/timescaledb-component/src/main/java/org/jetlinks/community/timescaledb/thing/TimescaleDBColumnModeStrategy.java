package org.jetlinks.community.timescaledb.thing;

import org.jetlinks.community.things.data.TableSafeMetricBuilder;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.DDLOperations;
import org.jetlinks.community.things.data.operations.QueryOperations;
import org.jetlinks.community.things.data.operations.SaveOperations;
import org.jetlinks.community.timescaledb.TimescaleDBOperations;

public class TimescaleDBColumnModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final TimescaleDBOperations operations;
    private final TimescaleDBThingsDataProperties properties;

    public TimescaleDBColumnModeStrategy(ThingsRegistry registry,
                                         TimescaleDBOperations operations) {
        this(registry, operations, new TimescaleDBThingsDataProperties());
    }

    public TimescaleDBColumnModeStrategy(ThingsRegistry registry,
                                         TimescaleDBOperations operations,
                                         TimescaleDBThingsDataProperties properties) {
        this.registry = registry;
        this.operations = operations;
        this.properties = properties;
    }

    @Override
    public String getId() {
        return "timescaledb-column";
    }

    @Override
    public String getName() {
        return "TimescaleDB-多列模式";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new TimescaleDBColumnModeSaveOperations(
            registry,
            TableSafeMetricBuilder.of(context.getMetricBuilder()),
            context.getSettings(),
            operations.writer());
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TimescaleDBColumnModeQueryOperations(
            thingType,
            templateId,
            thingId,
            TableSafeMetricBuilder.of(context.getMetricBuilder()),
            context.getSettings(),
            registry,
            operations.database());
    }

    @Override
    protected DDLOperations createForDDL(String thingType,
                                         String templateId,
                                         String thingId,
                                         OperationsContext context) {
        return new TimescaleDBColumnModeDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            TableSafeMetricBuilder.of(context.getMetricBuilder()),
            operations.database(),
            properties);
    }

    @Override
    public int getOrder() {
        return 10070;
    }
}

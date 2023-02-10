package org.jetlinks.community.things.data;

import lombok.AllArgsConstructor;
import org.jetlinks.community.things.data.operations.*;

public abstract class AbstractThingDataRepositoryStrategy extends CacheSaveOperationsStrategy {

    @Override
    public abstract SaveOperations createOpsForSave(OperationsContext context);


    protected abstract QueryOperations createForQuery(String thingType,
                                                      String templateId,
                                                      String thingId,
                                                      OperationsContext context);

    protected abstract DDLOperations createForDDL(String thingType,
                                                  String templateId,
                                                  String thingId,
                                                  OperationsContext context);

    @Override
    public final ThingOperations opsForThing(String thingType,
                                             String templateId,
                                             String thingId,
                                             OperationsContext context) {
        return new ThingOperationsHolder(thingType, templateId, thingId, context);
    }

    @Override
    public final TemplateOperations opsForTemplate(String thingType,
                                                   String templateId,
                                                   OperationsContext context) {
        return new ThingOperationsHolder(thingType, templateId, null, context);
    }


    @AllArgsConstructor
    class ThingOperationsHolder implements ThingOperations, TemplateOperations {
        private final String thingType;
        private final String templateId;
        private final String thingId;
        private final OperationsContext context;

        @Override
        public QueryOperations forQuery() {
            return createForQuery(thingType, templateId, thingId, context);
        }


        @Override
        public DDLOperations forDDL() {
            return createForDDL(thingType, templateId, thingId, context);
        }
    }


}

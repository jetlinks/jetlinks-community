package org.jetlinks.community.dashboard;

public interface MeasurementDefinition extends Definition {
    static MeasurementDefinition of(String id, String name) {
        return new MeasurementDefinition() {

            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

}
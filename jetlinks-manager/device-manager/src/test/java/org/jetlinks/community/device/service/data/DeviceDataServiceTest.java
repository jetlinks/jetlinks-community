package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceDataServiceTest {

    @Test
    void deviceProperty() {

        DeviceDataService.DevicePropertyAggregation aggregation = new DeviceDataService.DevicePropertyAggregation();
        aggregation.setProperty("test");
        aggregation.setAlias("");
        String alias = aggregation.getAlias();
        assertNotNull(alias);
    }
    @Test
    void aggregationRequest(){
        DeviceDataService.AggregationRequest build = DeviceDataService.AggregationRequest.builder()
            .from(new Date())
            .to(new Date())
            .limit(30)
            .build();
        Date from = build.getFrom();
        assertNotNull(from);
        Date to = build.getTo();
        assertNotNull(to);
        int limit = build.getLimit();
        assertEquals(30,limit);
        build.setQuery(new QueryParamEntity());

    }
    @Test
    void properties(){
        DeviceDataStorageProperties properties = new DeviceDataStorageProperties();
        properties.setDefaultPolicy("test");
    }


}
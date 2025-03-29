package org.jetlinks.community.device.web.request;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.springframework.util.Assert;

import java.util.List;

@Getter
@Setter
public class AggRequest {
    private List<DeviceDataService.DevicePropertyAggregation> columns;

    private DeviceDataService.AggregationRequest query;

    public void validate(){
        Assert.notNull(columns,"columns can not be null");
        Assert.notNull(query,"query can not be null");
        Assert.notEmpty(columns,"columns can not be empty");
        columns.forEach(DeviceDataService.DevicePropertyAggregation::validate);
    }
}
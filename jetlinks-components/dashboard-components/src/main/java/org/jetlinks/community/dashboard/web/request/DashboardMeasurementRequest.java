package org.jetlinks.community.dashboard.web.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DashboardMeasurementRequest {

    private String group;

    private String dashboard;

    private String object;

    private String measurement;

    private String dimension;

    private Map<String,Object> params;

}

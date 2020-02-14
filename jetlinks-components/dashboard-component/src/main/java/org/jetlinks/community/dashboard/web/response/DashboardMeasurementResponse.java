package org.jetlinks.community.dashboard.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.dashboard.MeasurementValue;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DashboardMeasurementResponse {

    private String group;

    private MeasurementValue data;


}

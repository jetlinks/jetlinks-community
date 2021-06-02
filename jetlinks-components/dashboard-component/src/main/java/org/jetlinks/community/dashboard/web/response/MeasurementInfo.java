package org.jetlinks.community.dashboard.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.core.metadata.DataType;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
public class MeasurementInfo {

    private String id;

    private String name;

    private DataType type;

    private List<DimensionInfo> dimensions;

    public static Mono<MeasurementInfo> of(Measurement measurement){
        return measurement.getDimensions()
            .map(DimensionInfo::of)
            .collectList()
            .map(list->{
                MeasurementInfo info=new MeasurementInfo();
                info.setId(measurement.getDefinition().getId());
                info.setName(measurement.getDefinition().getName());
                info.setDimensions(list);
                return info;
            });
    }
}

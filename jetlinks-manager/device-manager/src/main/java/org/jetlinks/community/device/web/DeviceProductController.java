package org.jetlinks.community.device.web;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/device-product")
@Resource(id = "device-product", name = "设备型号")
public class DeviceProductController implements ReactiveServiceCrudController<DeviceProductEntity, String> {

    @Autowired
    private LocalDeviceProductService productService;

    @Autowired
    private TimeSeriesManager timeSeriesManager;

    @Override
    public LocalDeviceProductService getService() {
        return productService;
    }

    @PostMapping({
        "/deploy/{productId:.+}",//todo 即将移除
        "/{productId:.+}/deploy"
    })
    public Mono<Integer> deviceDeploy(@PathVariable String productId) {
        return productService.deploy(productId);
    }

    @PostMapping({
        "/cancelDeploy/{productId:.+}",//todo 即将移除
        "/{productId:.+}/undeploy"
    })
    public Mono<Integer> cancelDeploy(@PathVariable String productId) {
        return productService.cancelDeploy(productId);
    }

    /**
     * 查询指定设备型号的事件数据
     *
     * @param queryParam 查询条件
     * @param productId  型号ID
     * @param eventId    事件标识
     * @return 查询结果
     */
    @GetMapping("{productId}/event/{eventId}")
    @QueryAction
    public Mono<PagerResult<Map<String, Object>>> queryPagerByDeviceEvent(QueryParam queryParam,
                                                                          @PathVariable String productId,
                                                                          @PathVariable String eventId) {
        return timeSeriesManager
            .getService(DeviceTimeSeriesMetric.deviceEventMetric(productId, eventId))
            .queryPager(queryParam, TimeSeriesData::getData);
    }

}

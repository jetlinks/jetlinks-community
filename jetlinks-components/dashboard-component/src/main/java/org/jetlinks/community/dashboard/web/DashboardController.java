package org.jetlinks.community.dashboard.web;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.web.request.DashboardMeasurementRequest;
import org.jetlinks.community.dashboard.web.response.DashboardInfo;
import org.jetlinks.community.dashboard.web.response.DashboardMeasurementResponse;
import org.jetlinks.community.dashboard.web.response.MeasurementInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@Resource(id="dashboard",name = "仪表盘")
@Authorize
public class DashboardController {

    private final DashboardManager dashboardManager;

    public DashboardController(DashboardManager dashboardManager) {
        this.dashboardManager = dashboardManager;
    }

    @GetMapping("/defs")
    @QueryAction
    public Flux<DashboardInfo> getDefinitions() {
        return dashboardManager
            .getDashboards()
            .flatMap(DashboardInfo::of);
    }

    @GetMapping("/def/{dashboard}/{object}/measurements")
    @QueryAction
    public Flux<MeasurementInfo> getMeasurementDefinitions(@PathVariable String dashboard,
                                                           @PathVariable String object) {
        return dashboardManager
            .getDashboard(dashboard)
            .flatMap(dash -> dash.getObject(object))
            .flatMapMany(DashboardObject::getMeasurements)
            .flatMap(MeasurementInfo::of);
    }

    @GetMapping(value = "/{dashboard}/{object}/{measurement}/{dimension}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Authorize(merge = false)
    public Flux<MeasurementValue> getMeasurementValue(@PathVariable String dashboard,
                                                      @PathVariable String object,
                                                      @PathVariable String dimension,
                                                      @PathVariable String measurement,
                                                      @RequestParam Map<String, Object> params) {
        return dashboardManager
            .getDashboard(dashboard)
            .flatMap(dash -> dash.getObject(object))
            .flatMap(obj -> obj.getMeasurement(measurement))
            .flatMap(meas -> meas.getDimension(dimension))
            .switchIfEmpty(Mono.error(() -> new NotFoundException("不支持的仪表盘")))
            .flatMapMany(dim -> dim.getValue(MeasurementParameter.of(params)));
    }

    /**
     * POST 方式批量获取仪表数据,不支持获取实时数据.
     *
     * @param requests 请求参数
     * @return 仪表数据
     */
    @PostMapping(value = "/_multi")
    @Authorize(merge = false)
    public Flux<DashboardMeasurementResponse> getMultiMeasurementValue(@RequestBody Flux<DashboardMeasurementRequest> requests) {
        return requests.flatMap(request -> dashboardManager
            .getDashboard(request.getDashboard())
            .flatMap(dash -> dash.getObject(request.getObject()))
            .flatMap(obj -> obj.getMeasurement(request.getMeasurement()))
            .flatMap(meas -> meas.getDimension(request.getDimension()))
            .filter(dim -> !dim.isRealTime()) //实时数据请使用EventSource方式
            .flatMapMany(dim -> dim.getValue(MeasurementParameter.of(request.getParams())))
            .map(val -> DashboardMeasurementResponse.of(request.getGroup(), val)));
    }

    /**
     * 使用EventSource方式批量获取仪表数据,支持获取实时数据.
     *
     * @param requestJson 请求集合json
     * @return 仪表数据
     */
    @GetMapping(value = "/_multi", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Authorize(merge = false)
    public Flux<DashboardMeasurementResponse> getMultiMeasurementValue(@RequestParam String requestJson) {
        return Flux.fromIterable(JSON.parseArray(requestJson, DashboardMeasurementRequest.class))
            .flatMap(request -> dashboardManager
                .getDashboard(request.getDashboard())
                .flatMap(dash -> dash.getObject(request.getObject()))
                .flatMap(obj -> obj.getMeasurement(request.getMeasurement()))
                .flatMap(meas -> meas.getDimension(request.getDimension()))
                .flatMapMany(dim -> dim.getValue(MeasurementParameter.of(request.getParams())))
                .map(val -> DashboardMeasurementResponse.of(request.getGroup(), val)));
    }
}

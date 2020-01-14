package org.jetlinks.community.device.web;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.LogService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/device-product")
@Resource(id = "device-product", name = "设备型号")
public class DeviceProductController implements ReactiveServiceCrudController<DeviceProductEntity, String> {

    private final LocalDeviceProductService productService;

    private final LogService logService;

    public DeviceProductController(LocalDeviceProductService productService, LogService logService) {
        this.productService = productService;
        this.logService = logService;
    }

    @Override
    public LocalDeviceProductService getService() {
        return productService;
    }

    @PostMapping("/deploy/{productId:.+}")
    public Mono<Integer> deviceDeploy(@PathVariable String productId) {
        return productService.deploy(productId);
    }

    @PostMapping("/cancelDeploy/{productId:.+}")
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
    public Mono<PagerResult<Map>> queryPagerByDeviceEvent(QueryParam queryParam,
                                                          @PathVariable String productId,
                                                          @PathVariable String eventId) {
        return logService.queryPagerByDeviceEvent(queryParam, productId, eventId);
    }

}

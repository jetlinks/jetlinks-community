package org.jetlinks.community.rule.engine.executor.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.core.things.Thing;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingTemplate;
import org.jetlinks.core.things.ThingsRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 设备选择器描述类
 * <p>
 * 例:
 * <p>
 * 固定设备:<pre>{@code
 *
 *     {
 *         "selector":"fixed",
 *         "source":"fixed", //设备ID来源为固定值
 *         "selectorValues":[{"value":"设备ID"}]
 *     }
 *
 *  }</pre>
 * <p>
 * 和上游相同的设备:<pre>{@code
 *    {
 *        "selector":"fixed",
 *        "source":"upper", //设备ID来源为上游
 *        "upperKey":"deviceId"
 *    }
 *  }</pre>
 * <p>
 * 标签<code>key</code>为<code>value</code>的设备 :<pre>{@code
 *    {
 *        "selector":"tag",
 *        "source":"fixed", //标签来源来自固定值
 *        "selectorValues":[{"value":{"标签key":"标签value"}}]
 *    }
 *  }</pre>
 * <p>
 * 与上游设备存在关系的设备 :<pre>{@code
 *    {
 *        "selector":"relation",
 *        "source":"upper", //设备ID来源为上游
 *        "upperKey":"deviceId"
 *        "selectorValues":[
 *              {
 *                  "value":{
 *                       "objectType":"user",   // 关系类型
 *                       "relation":"manager"   // 关系定义ID
 *                  }
 *               }
 *          ]
 *    }
 *  }</pre>
 * <p>
 *
 * @see DeviceSelectorProvider
 * @see DeviceSelectorProviders
 */
@Getter
@Setter
public class DeviceSelectorSpec extends VariableSource {
    /**
     * @see DeviceSelectorProvider#getProvider()
     */
    @Schema(description = "选择器标识")
    @NotBlank
    private String selector;

    /**
     * <pre>{@code
     *
     *  // org.hswebframework.ezorm.core.param.Term
     * 设备标签 : [{"value":[{"column":"tagKey","value":"tagValue"}],"name":"标签说明"}]
     *
     * 固定设备 : [{"value":"设备ID","name":"设备名称"}]
     *
     * 按部门 : [{"value":"部门ID","name":"部门名称"}]
     * }</pre>
     *
     * @see SelectorValue
     */
    @Schema(description = "选择器的值,如选择的部门ID,标签信息等")
    private List<SelectorValue> selectorValues;

    @Schema(description = "固定设备ID")
    @Override
    public Object getValue() {
        return super.getValue();
    }

    public DeviceSelectorSpec() {
    }

    private DeviceSelectorSpec(String selector, List<SelectorValue> selectorValue) {
        this.selector = selector;
        super.setValue(selectorValue);
    }


    /**
     * @param selector      selector
     * @param selectorValue value
     * @see DeviceSelectorProvider#getProvider()
     */
    public static DeviceSelectorSpec selector(@NotNull String selector, List<SelectorValue> selectorValue) {
        return new DeviceSelectorSpec(selector, selectorValue);
    }

    public Flux<Object> resolveSelectorValues(Map<String, Object> context) {
        if (CollectionUtils.isNotEmpty(selectorValues)) {
            return Flux
                .fromIterable(selectorValues)
                .mapNotNull(SelectorValue::getValue);
        }
        return super.resolve(context);
    }


    @Override
    public void validate() {
        DeviceSelectorProviders.getProviderNow(selector);
    }

    public Mono<ThingMetadata> getDeviceMetadata(ThingsRegistry registry, String productId) {
        //固定设备时使用设备的物模型
        if (getSource() == Source.fixed && DeviceSelectorProviders.PROVIDER_FIXED.equals(getSelector())) {
            List<SelectorValue> fixed = getSelectorValues();
            if (CollectionUtils.isNotEmpty(fixed) && fixed.size() == 1) {
                return registry
                    .getThing(DeviceThingType.device,String.valueOf(fixed.get(0).getValue()))
                    .flatMap(Thing::getMetadata);
            }
        }
        return registry
            .getTemplate(DeviceThingType.device,productId)
            .flatMap(ThingTemplate::getMetadata);
    }
}


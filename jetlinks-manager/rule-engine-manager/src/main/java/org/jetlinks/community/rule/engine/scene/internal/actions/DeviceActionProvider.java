package org.jetlinks.community.rule.engine.scene.internal.actions;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.rule.engine.executor.DeviceMessageSendTaskExecutorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@AllArgsConstructor
public class DeviceActionProvider implements SceneActionProvider<DeviceAction> {
    public static final String PROVIDER = "device";

    private final ThingsRegistry registry;

    @Override
    public String getProvider() {
        return DeviceActionProvider.PROVIDER;
    }

    @Override
    public DeviceAction newConfig() {
        return new DeviceAction();
    }

    @Override
    public List<String> parseColumns(DeviceAction config) {

        return config.parseColumns();
    }

    @Override
    public Flux<Variable> createVariable(DeviceAction config) {
        return config
            .getDeviceMetadata(registry, config.getProductId())
            .as(LocaleUtils::transform)
            .flatMapIterable(config::createVariables);
    }

    @Override
    public void applyRuleNode(DeviceAction device, RuleNodeModel model) {
        DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig config = new DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig();

        config.setMessage(device.getMessage());

        if (DeviceSelectorProviders.isFixed(device)) {
            config.setSelectorSpec(FastBeanCopier.copy(device, new DeviceSelectorSpec()));
        } else {
            config.setSelectorSpec(
                DeviceSelectorProviders.composite(
                    //先选择产品下的设备
                    DeviceSelectorProviders.product(device.getProductId()),
                    FastBeanCopier.copy(device, new DeviceSelectorSpec())
                ));
        }

        config.setFrom("fixed");
        config.setStateOperator("direct");
        config.setProductId(device.getProductId());

        model.setExecutor(DeviceMessageSendTaskExecutorProvider.EXECUTOR);
        model.setConfiguration(config.toMap());
    }
}

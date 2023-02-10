package org.jetlinks.community.rule.engine.executor.device;

import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.reactor.ql.utils.CastUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DeviceSelectorProviders {

    private final static Map<String, DeviceSelectorProvider> providers = new LinkedHashMap<>();

    public static final String
        PROVIDER_FIXED = "fixed",
        PROVIDER_CONTEXT = "context",
        PROVIDER_PRODUCT = "product",
        PROVIDER_TAG = "tag";

    static {

        register(SimpleDeviceSelectorProvider
                     .of(
                         "all", "全部设备",
                         (args, query) -> query));

        { //固定设备,fixed和context作用和效果完全相同,只是为了前端方便区分不同的操作

              /*
            选择固定的设备
            {
             "selector":"fixed",
             "selectorValues":[ {"value":"deviceId","name":"设备名称"} ],
            }
             */
            register(SimpleDeviceSelectorProvider
                         .of(
                             PROVIDER_FIXED, "固定设备",
                             (args, query) -> query.in("id", args)));
            /*
            根据上下文变量选择设备
            {
             "selector":"context",
             "source":"upper",
             "upperKey":"deviceId"
            }
             */
            register(SimpleDeviceSelectorProvider
                         .of(
                             PROVIDER_CONTEXT, "内置参数",
                             (args, query) -> query.in("id", args)));
        }


        register(SimpleDeviceSelectorProvider
                     .of("state", "按状态",
                         (args, query) -> query.in("state", args)));

        register(SimpleDeviceSelectorProvider
                     .of(PROVIDER_PRODUCT, "按产品",
                         (args, query) -> query.in("productId", args)));

        register(SimpleDeviceSelectorProvider
                     .of(PROVIDER_TAG, "按标签",
                         (args, query) -> {
                             if (args.size() == 1) {
                                 return query.accept("id",
                                                     "dev-tag",
                                                     args.get(0));
                             }
                             Map<Object, Object> tagsMap = Maps.newLinkedHashMapWithExpectedSize(args.size());
                             List<Object> temp = new ArrayList<>(args.size());
                             for (Object val : args) {
                                 // {key:value}
                                 if (val instanceof Map) {
                                     tagsMap.putAll(((Map) val));
                                 }
                                 // key:value
                                 else if (val instanceof String) {
                                     String[] arr = String.valueOf(val).split(":");
                                     if (arr.length == 2) {
                                         tagsMap.put(arr[0], arr[1]);
                                     }
                                 } else {
                                     temp.add(val);
                                 }
                             }
                             if (CollectionUtils.isNotEmpty(temp)) {
                                 tagsMap.putAll(CastUtils.castMap(temp));
                             }
                             return query.accept("id",
                                                 "dev-tag",
                                                 tagsMap);
                         }));

        register(new CompositeDeviceSelectorProvider());

    }

    //判断是否为固定设备选择器，固定设备选择器不需要执行查询库,性能更高
    public static boolean isFixed(DeviceSelectorSpec spec) {
        return PROVIDER_FIXED.equals(spec.getSelector()) ||
            PROVIDER_CONTEXT.equals(spec.getSelector());
    }

    public static DeviceSelectorSpec fixed(Object value) {
        DeviceSelectorSpec spec = new DeviceSelectorSpec();
        spec.setSelector(PROVIDER_CONTEXT);
        spec.setSource(VariableSource.Source.fixed);
        spec.setValue(value);
        return spec;
    }

    public static DeviceSelectorSpec product(String productId) {
        DeviceSelectorSpec spec = new DeviceSelectorSpec();
        spec.setSelector(PROVIDER_PRODUCT);
        spec.setSource(VariableSource.Source.fixed);
        spec.setValue(productId);
        return spec;
    }

    public static DeviceSelectorSpec composite(Collection<DeviceSelectorSpec> specs) {
        DeviceSelectorSpec composite = new DeviceSelectorSpec();
        composite.setSource(VariableSource.Source.fixed);
        composite.setSelector(CompositeDeviceSelectorProvider.PROVIDER);
        composite.setSelectorValues(specs
                                        .stream()
                                        .map(spec -> SelectorValue.of(spec, null))
                                        .collect(Collectors.toList()));
        return composite;

    }

    public static DeviceSelectorSpec composite(DeviceSelectorSpec... specs) {
        return composite(Arrays.asList(specs));
    }

    public static void register(DeviceSelectorProvider provider) {
        providers.put(provider.getProvider(), provider);
    }

    public static Optional<DeviceSelectorProvider> getProvider(String provider) {
        return Optional.ofNullable(providers.get(provider));
    }

    public static DeviceSelectorProvider getProviderNow(String provider) {
        return getProvider(provider)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported device selector provider:" + provider));
    }

    public static List<DeviceSelectorProvider> allProvider() {
        return new ArrayList<>(providers.values());
    }

}
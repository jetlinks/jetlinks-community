package org.jetlinks.community.things;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactor.function.Consumer3;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.things.data")
public class ThingsDataProperties {
    private Set<String> autoUpdateThingTypes = new HashSet<>();

    /**
     * 存储相关配置
     */
    private Store store = new Store();

    @Getter
    @Setter
    public static class Store {
        //每个属性保存记录最大数量
        private int maxSizeEachProperty = 8;
    }
}

package org.jetlinks.community.device.web.excel;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bestfeng
 */
@ConfigurationProperties(prefix = "jetlinks.import.filter.device")
@Component
@Getter
@Setter
@Generated
public class DeviceExcelFilterColumns {

    private List<String> columns = new ArrayList<>();

}

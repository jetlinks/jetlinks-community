package org.jetlinks.community.things.helper.modbus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽象的物模型写入操作定义。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ThingWriteOperation {

    /**
     * 物模型属性标识
     */
    private String property;

    /**
     * 写入的目标值
     */
    private Object value;

}


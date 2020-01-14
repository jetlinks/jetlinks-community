package org.jetlinks.community.elastic.search.index.mapping;

import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.index.CreateIndex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public class MappingFactory {


    private Map<String, Object> properties = new HashMap<>();

    private Map<String, Object> filedMap = new HashMap<>();

    private volatile boolean flag = true;

    private CreateIndex index;

    private MappingFactory(CreateIndex index) {
        this.index = index;
    }

    public MappingFactory addFieldName(String fieldName) {
        continuityOperateHandle(!flag);
        flag = false;
        filedMap = new HashMap<>();
        properties.put(fieldName, filedMap);
        return this;
    }

    public MappingFactory addFieldType(FieldType type) {
        continuityOperateHandle(flag);
        filedMap.put("type", type.getValue());
        return this;
    }

    public MappingFactory addFieldDateFormat(FieldDateFormat... dateFormats) {
        continuityOperateHandle(flag);
        filedMap.put("format", FieldDateFormat.getFormatStr(Arrays.asList(dateFormats)));
        return this;
    }

    public MappingFactory commit() {
        flag = true;
        return this;
    }

    public CreateIndex end() {
        index.setMapping(properties);
        return index;
    }

    public static MappingFactory createInstance(CreateIndex index) {
        return new MappingFactory(index);
    }

    private void continuityOperateHandle(boolean inoperable) {
        if (inoperable) {
            throw new BusinessException("please exec commit() or addFiledName() later then operate");
        }
    }

}

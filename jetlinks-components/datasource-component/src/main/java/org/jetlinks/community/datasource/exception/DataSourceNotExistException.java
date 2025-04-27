package org.jetlinks.community.datasource.exception;

import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.community.datasource.DataSourceType;

public class DataSourceNotExistException extends I18nSupportException {

    public DataSourceNotExistException(DataSourceType datasourceType, String dataSourceId) {
        this(datasourceType.getId(), dataSourceId);
    }

    public DataSourceNotExistException(String datasourceType, String dataSourceId) {
        super("error.datasource_not_exist", datasourceType, dataSourceId);
    }
}

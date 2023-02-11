package org.jetlinks.community.tdengine;

import lombok.Generated;
import lombok.Getter;

@Generated
public class TDengineException extends RuntimeException {

    @Getter
    private final String sql;

    @Generated
    public TDengineException(String sql, String message) {
        super(message);
        this.sql = sql;
    }
    @Generated
    public TDengineException(String sql, String message, Throwable cause) {
        super(message, cause);
        this.sql = sql;
    }
}

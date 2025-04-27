package org.jetlinks.community.datasource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

@AllArgsConstructor
@Getter
public class DataSourceState {
    //正常
    public static String code_ok = "ok";
    //正常
    public static String code_stopped = "stopped";
    //正常
    public static String code_error = "error";

    public static final DataSourceState ok = new DataSourceState(code_ok, null);
    public static final DataSourceState stopped = new DataSourceState(code_stopped, null);

    private final String code;

    private final Throwable reason;

    public boolean isOk(){
        return code_ok.equals(code);
    }

    public static DataSourceState error(Throwable error) {
        return new DataSourceState(code_error, error);
    }

    @SneakyThrows
    public void validate() {
        if (reason != null) {
            throw reason;
        }
    }
}

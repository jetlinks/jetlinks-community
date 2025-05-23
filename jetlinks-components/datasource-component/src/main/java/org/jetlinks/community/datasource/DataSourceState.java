/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

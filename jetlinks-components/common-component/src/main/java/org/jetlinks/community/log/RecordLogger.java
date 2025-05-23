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
package org.jetlinks.community.log;

import org.jetlinks.core.monitor.logger.Logger;
import org.slf4j.event.Level;

import java.util.function.Supplier;

public abstract class RecordLogger implements Logger {

    @Override
    public final void log(Level level, String message, Object... args) {

        handleLog(() -> new LogRecord().withLog(level, message, args));
    }


    protected abstract void handleLog(Supplier<LogRecord> supplier);

}

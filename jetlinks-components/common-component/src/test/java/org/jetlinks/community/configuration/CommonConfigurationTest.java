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
package org.jetlinks.community.configuration;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonConfigurationTest {

    static {
        try {
            Class.forName(CommonConfiguration.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void testTimestamp() {
        long timestamp = 1717200000000L;
        BeanUtilsBean beanUtils = BeanUtilsBean.getInstance();

        assertEquals(timestamp, ((Timestamp) beanUtils
            .getConvertUtils()
            .convert(new Date(timestamp), Timestamp.class)).getTime());

        assertEquals(timestamp, ((Timestamp) beanUtils
            .getConvertUtils()
            .convert(timestamp, Timestamp.class)).getTime());

        assertEquals(timestamp, ((Timestamp) beanUtils
            .getConvertUtils()
            .convert(String.valueOf(timestamp), Timestamp.class)).getTime());

        assertEquals(Timestamp.valueOf("2026-05-22 10:20:30"), beanUtils
            .getConvertUtils()
            .convert("2026-05-22 10:20:30", Timestamp.class));
    }
}

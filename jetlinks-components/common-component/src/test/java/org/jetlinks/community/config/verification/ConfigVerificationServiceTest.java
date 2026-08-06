/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.config.verification;

import org.hswebframework.web.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

class ConfigVerificationServiceTest {

    private final ConfigVerificationService service = new ConfigVerificationService();

    @ParameterizedTest
    @ValueSource(strings = {
        "http://127.0.0.1:28081",
        "http://localhost:28081",
        "http://localhost.:28081",
        "http://[::1]:28081"
    })
    void shouldRejectLoopbackBasePath(String basePath) {
        StepVerifier
            .create(service.doBasePathValidate(basePath))
            .expectError(BusinessException.class)
            .verify();
    }
}

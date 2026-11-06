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
package org.jetlinks.community.auth.captcha.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageCaptchaProviderTest {

    @Test
    void clampKeepsDefaultAndBoundarySizes() {
        assertEquals(130, ImageCaptchaProvider.clampWidth(130));
        assertEquals(40, ImageCaptchaProvider.clampHeight(40));
        assertEquals(75, ImageCaptchaProvider.clampWidth(75));
        assertEquals(260, ImageCaptchaProvider.clampWidth(260));
        assertEquals(20, ImageCaptchaProvider.clampHeight(20));
        assertEquals(80, ImageCaptchaProvider.clampHeight(80));
    }

    @Test
    void clampLimitsOversizedAndUndersizedRequests() {
        assertEquals(75, ImageCaptchaProvider.clampWidth(0));
        assertEquals(75, ImageCaptchaProvider.clampWidth(-1));
        assertEquals(75, ImageCaptchaProvider.clampWidth(Integer.MIN_VALUE));
        assertEquals(260, ImageCaptchaProvider.clampWidth(15000));
        assertEquals(260, ImageCaptchaProvider.clampWidth(Integer.MAX_VALUE));

        assertEquals(20, ImageCaptchaProvider.clampHeight(0));
        assertEquals(20, ImageCaptchaProvider.clampHeight(-1));
        assertEquals(20, ImageCaptchaProvider.clampHeight(Integer.MIN_VALUE));
        assertEquals(80, ImageCaptchaProvider.clampHeight(15000));
        assertEquals(80, ImageCaptchaProvider.clampHeight(Integer.MAX_VALUE));
    }
}

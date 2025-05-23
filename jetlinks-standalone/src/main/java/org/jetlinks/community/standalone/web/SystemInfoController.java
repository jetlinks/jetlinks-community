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
package org.jetlinks.community.standalone.web;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.community.Version;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("/system")
@RestController
@AllArgsConstructor
public class SystemInfoController {

    private final ApiInfoProperties apiInfoProperties;

    @GetMapping("/version")
    @Authorize(ignore = true)
    public Mono<Version> getVersion() {
        return Mono.just(Version.current);
    }

    @GetMapping("/apis")
    @Authorize(ignore = true)
    public Mono<ApiInfoProperties> getApis() {
        return Mono.just(apiInfoProperties);
    }

}

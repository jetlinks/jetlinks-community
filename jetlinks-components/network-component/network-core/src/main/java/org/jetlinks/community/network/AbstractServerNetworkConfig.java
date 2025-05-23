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
package org.jetlinks.community.network;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.jetlinks.community.network.resource.NetworkTransport;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public abstract class AbstractServerNetworkConfig implements ServerNetworkConfig {

    @NotBlank
    protected String id;

    protected boolean publicSecure;

    protected String publicCertId;

    @NotBlank
    protected String publicHost;

    @Range(min = 1, max = 65535)
    protected int publicPort;

    @NotBlank
    protected String host = "0.0.0.0";

    @Range(min = 1, max = 65535)
    protected int port;

    protected boolean secure;

    protected String certId;

    @Override
    public abstract NetworkTransport getTransport();


}

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
package org.jetlinks.community.device.web.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TransparentMessageEncodeResponse {

    private boolean success;

    private String reason;

    /** 每个编码帧的十六进制字符串，格式如 "01 10 00 00 00 02 04 43 C8 00 00" */
    private List<String> frames;

    public static TransparentMessageEncodeResponse of(List<String> frames) {
        TransparentMessageEncodeResponse resp = new TransparentMessageEncodeResponse();
        resp.success = true;
        resp.frames = frames;
        return resp;
    }

    public static TransparentMessageEncodeResponse error(String reason) {
        TransparentMessageEncodeResponse resp = new TransparentMessageEncodeResponse();
        resp.success = false;
        resp.reason = reason;
        return resp;
    }
}

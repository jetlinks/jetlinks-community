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
package org.jetlinks.community.notify.email.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ParsedEmailTemplate {

    //附件 key:附件名称 value:附件uri
    private Map<String, String> attachments;

    //图片 key:text中图片占位符 value:图片uri
    private Map<String, String> images;

    //邮件主题
    private String subject;

    //邮件内容
    private String text;

    //发送人集合
    private List<String> sendTo;
}

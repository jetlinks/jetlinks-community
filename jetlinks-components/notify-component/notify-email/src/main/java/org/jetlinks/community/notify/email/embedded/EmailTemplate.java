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

import lombok.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.notify.template.AbstractTemplate;
import org.jetlinks.community.notify.template.VariableDefinition;
import org.jetlinks.community.relation.RelationConstants;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.FileType;
import org.jetlinks.community.notify.NotifyVariableBusinessConstant;
import org.jetlinks.community.notify.template.Variable;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@Setter
public class EmailTemplate extends AbstractTemplate<EmailTemplate> {

    private static final String SEND_TO_KEY = "sendTo";

    private String subject;

    private String text;

    private List<Attachment> attachments;

    private List<String> sendTo;

    public Mono<List<String>> getSendTo(Map<String, Object> context) {
        return VariableSource
            .resolveValue(SEND_TO_KEY, context, RelationConstants.UserProperty.email)
            .flatMapIterable(e -> ConverterUtils.convertToList(e, String::valueOf))
            .concatWith(Mono.justOrEmpty(sendTo).flatMapIterable(Function.identity()))
            .collectList();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(of = "name")
    public static class Attachment {

        public static final String LOCATION_KEY = "location";

        private String name;

        private String location;

        public static String locationKey(int index) {
            return "_attach_location_" + index;
        }

        public static String locationName(int index) {
            return "_attach_name_" + index;
        }
    }


    @Nonnull
    @Override
    protected List<VariableDefinition> getEmbeddedVariables() {
        List<VariableDefinition> variables = new ArrayList<>();
        if (CollectionUtils.isEmpty(sendTo)) {
            variables.add(
                VariableDefinition
                    .builder()
                    .id(SEND_TO_KEY)
                    .name("收件人")
                    .expand(NotifyVariableBusinessConstant.businessId,
                        NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.userType)
                    .required(true)
                    .type(ArrayType.ID)
                    .build()
            );
        }

        List<Attachment> attachments = getAttachments();
        if (!CollectionUtils.isEmpty(attachments)) {
            int index = 0;
            for (Attachment attachment : attachments) {
                index++;
                if (StringUtils.isNotEmpty(attachment.getName())
                    && StringUtils.isEmpty(attachment.getLocation())) {
                    variables.add(
                        VariableDefinition
                            .builder()
                            .id(Attachment.locationKey(index))
                            .name(attachment.getName())
                            .type(FileType.ID)
                            .description(attachment.getName())
                            .format(Variable.FileFormat.any)
                            .required(true)
                            .build()
                    );
                }
            }
        }
        return variables;
    }
}

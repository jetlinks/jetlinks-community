package org.jetlinks.community.notify.email.embedded;

import lombok.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.notify.NotifyVariableBusinessConstant;
import org.jetlinks.community.notify.template.AbstractTemplate;
import org.jetlinks.community.notify.template.VariableDefinition;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.FileType;
import org.jetlinks.community.notify.template.Variable;
import org.jetlinks.community.relation.RelationConstants;
import org.jetlinks.community.relation.utils.VariableSource;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jetlinks.community.notify.email.embedded.EmailTemplate.Attachment.LOCATION_KEY;

@Getter
@Setter
public class EmailTemplate extends AbstractTemplate<EmailTemplate> {

    private static final String SEND_TO_KEY = "sendTo";

    private String subject;

    private String text;

    private List<Attachment> attachments;

    private List<String> sendTo;

    public Mono<List<String>> getSendTo(Map<String, Object> context) {
        return VariableSource.resolveValue(SEND_TO_KEY, context, RelationConstants.UserProperty.email)
            .map(String::valueOf)
            .collect(Collectors.joining(","))
            .filter(StringUtils::isNoneBlank)
            .map(value -> Arrays.stream(value
                .split(","))
                .collect(Collectors.toList()))
            .switchIfEmpty(Mono.just(sendTo == null ? new ArrayList<>() : sendTo));
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
            variables.addAll(attachments
                .stream()
                //附件名称不为空，附件数据为空
                .filter(attachment -> StringUtils.isNotEmpty(attachment.getName())
                    && StringUtils.isEmpty(attachment.getLocation()))
                .map(attachment -> VariableDefinition
                    .builder()
                    .id(LOCATION_KEY)
                    .name(attachment.getName())
                    .type(FileType.ID)
                    .description(attachment.getName())
                    .format(Variable.FileFormat.any)
                    .required(true)
                    .build())
                .collect(Collectors.toList()));
        }
        return variables;
    }
}

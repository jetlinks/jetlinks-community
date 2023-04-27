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

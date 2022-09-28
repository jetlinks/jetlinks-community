package org.jetlinks.community.notify.sms.aliyun.expansion;

import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsTemplateListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class SmsTemplate {

    @Schema(description = "模板code")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板内容")
    private String templateContent;

    //0：短信通知。
    //1：推广短信。
    //2：验证码短信。
    //6：国际/港澳台短信。
    //7：数字短信。
    @Schema(description = "模板类型")
    private String templateType;



    //AUDIT_STATE_INIT：审核中。
    //AUDIT_STATE_PASS：审核通过。
    //AUDIT_STATE_NOT_PASS：审核未通过，请在返回参数Reason中查看审核未通过原因。
    //AUDIT_STATE_CANCEL：取消审核。
    @Schema(description = "审批状态")
    private String auditStatus;

    public static SmsTemplate of(QuerySmsTemplateListResponse.SmsStatsResultDTO dto) {
        return FastBeanCopier.copy(dto, SmsTemplate.class);
    }
}

package org.jetlinks.community.notify.sms.aliyun.expansion;

import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsSignListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class SmsSign {

    @Schema(description = "签名名称")
    private String signName;

    //AUDIT_STATE_INIT：审核中。
    //AUDIT_STATE_PASS：审核通过。
    //AUDIT_STATE_NOT_PASS：审核未通过，请在返回参数Reason中查看审核未通过原因。
    //AUDIT_STATE_CANCEL：取消审核。
    @Schema(description = "签名状态")
    private String auditStatus;

    @Schema(description = "创建时间")
    private String createDate;

    @Schema(description = "签名场景类型")
    private String businessType;


    public static SmsSign of(QuerySmsSignListResponse.QuerySmsSignDTO dto) {
        return FastBeanCopier.copy(dto, SmsSign.class);
    }
}

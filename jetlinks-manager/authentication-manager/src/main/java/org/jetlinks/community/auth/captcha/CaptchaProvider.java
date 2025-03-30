package org.jetlinks.community.auth.captcha;

import org.jetlinks.community.spi.Provider;
import reactor.core.publisher.Mono;

/**
 * 验证码提供商,实现此接口并注册为spring bean即可提供验证码服务.
 *
 * @author zhouhao
 * @see CaptchaProperties
 * @since 2.1
 */
public interface CaptchaProvider {

    Provider<CaptchaProvider> supports = Provider.create(CaptchaProvider.class);

    /**
     * @return 验证码类型
     */
    String getType();

    /**
     * 执行验证,如果验证失败,应当返回异常,如:
     * <pre>{@code
     *
     *  return Mono.error(new ValidationException("error.verification_code"));
     *
     * }</pre>
     *
     * @param context 校验上下文,可通过上下文来获取参数
     * @return 验证结果
     */
    Mono<Void> validate(ValidationContext context);

    /**
     * @return 提供给前端所需的配置信息
     */
    Object getConfigForFront();
}

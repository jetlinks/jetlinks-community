package org.jetlinks.community.network;

import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.network.resource.NetworkTransport;
import org.springframework.util.StringUtils;

/**
 * 网络组件配置
 *
 * @author zhouhao
 * @see ServerNetworkConfig
 * @see ClientNetworkConfig
 * @since 2.0
 */
public interface NetworkConfig {

    /**
     * @return 获取配置ID
     */
    String getId();

    /**
     *
     * @return 网络协议类型 TCP or UDP
     */
    NetworkTransport getTransport();

    /**
     * 传输模式,如: http,mqtt,ws
     * @return 传输模式
     */
    String getSchema();

    /**
     * 是否使用安全加密(TLS,DTLS)
     *
     * @return true or false
     */
    boolean isSecure();

    /**
     * 安全证书ID ,当{@link NetworkConfig#isSecure()}为true时,不能为空.
     *
     * @return 证书ID
     * @see org.jetlinks.community.network.security.Certificate
     * @see org.jetlinks.community.network.security.CertificateManager
     */
    String getCertId();

    /**
     * 验证配置,配置不合法将抛出{@link ValidationException}
     */
   default void validate(){
       ValidatorUtils.tryValidate(this);
       if (isSecure() && !StringUtils.hasText(getCertId())) {
           throw new ValidationException("certId", "validation.cert_id_can_not_be_empty");
       }
   }
}

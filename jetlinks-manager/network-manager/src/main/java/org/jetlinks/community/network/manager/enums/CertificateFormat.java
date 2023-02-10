package org.jetlinks.community.network.manager.enums;

import lombok.Getter;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.security.DefaultCertificate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

/**
 * 证书格式
 *
 * @author wangzheng
 * @since 1.0
 */
@Getter
public enum CertificateFormat {
    PFX {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            if (StringUtils.hasText(config.getKeystoreBase64())) {
                certificate
                    .initPfxKey(decodeBase64(config.getKeystoreBase64()), config.getKeystorePwd());
            }
            return certificate
                .initPfxTrust(decodeBase64(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    JKS {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            if (StringUtils.hasText(config.getKeystoreBase64())) {
                certificate
                    .initJksKey(decodeBase64(config.getKeystoreBase64()), config.getKeystorePwd());
            }
            return certificate
                .initJksTrust(decodeBase64(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    PEM {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {

            if (StringUtils.hasText(config.getKey())) {
                byte[] key = parseCertBody(config.getKey());
                byte[] cert = StringUtils.hasText(config.getCert()) ? parseCertBody(config.getCert()) : key;
                certificate.initPemKey(Collections.singletonList(key), Collections.singletonList(cert));
            }

            if (StringUtils.hasText(config.getTrust())) {
                certificate
                    .initPemTrust(Collections.singletonList(parseCertBody(config.getTrust())));
            }

            return certificate;
        }

        private byte[] parseCertBody(String text) {
            //明文证书
            if (text.contains("-")) {
                return text.getBytes(StandardCharsets.UTF_8);
            }
            return decodeBase64(text);
        }
    };

    public abstract DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config);
}

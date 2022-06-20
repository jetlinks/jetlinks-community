package org.jetlinks.community.network.manager.enums;

import lombok.Getter;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.security.DefaultCertificate;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

/**
 * @author wangzheng
 * @since 1.0
 */
@Getter
public enum CertificateType {
    PFX {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            return certificate
                .initPfxKey(decodeBase64(config.getKeystoreBase64()), config.getKeystorePwd())
                .initPfxTrust(decodeBase64(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    JKS {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            return certificate
                .initJksKey(decodeBase64(config.getKeystoreBase64()), config.getKeystorePwd())
                .initJksTrust(decodeBase64(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    PEM {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            List<byte[]> keyCert = Collections.singletonList(decodeBase64(config.getKeystoreBase64()));

            return certificate
                .initPemKey(keyCert, keyCert)
                .initPemTrust(Collections.singletonList(decodeBase64(config.getTrustKeyStoreBase64())));

        }
    };

    public abstract DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config);
}

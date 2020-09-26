package org.jetlinks.community.network.manager.enums;

import lombok.Getter;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.security.DefaultCertificate;

import java.util.*;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Getter
public enum CertificateType {
    PFX {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            return certificate
                .initPfxKey(Base64.getDecoder().decode(config.getKeystoreBase64()), config.getKeystorePwd())
                .initPfxTrust(Base64.getDecoder().decode(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    JKS {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            return certificate
                .initJksKey(Base64.getDecoder().decode(config.getKeystoreBase64()), config.getKeystorePwd())
                .initJksTrust(Base64.getDecoder().decode(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    PEM {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            List<byte[]> keyCert = Collections.singletonList(Base64.getDecoder().decode(config.getKeystoreBase64()));

            return certificate
                .initPemKey(keyCert, keyCert)
                .initPemTrust(Collections.singletonList(Base64.getDecoder().decode(config.getTrustKeyStoreBase64())));

        }
    };

    public abstract DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config);
}

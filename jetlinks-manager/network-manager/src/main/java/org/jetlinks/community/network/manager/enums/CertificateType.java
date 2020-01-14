package org.jetlinks.community.network.manager.enums;

import lombok.Getter;
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
        public DefaultCertificate init(DefaultCertificate certificate, Map<String, String> param) {
            return certificate
                    .initPfxKey(Base64.getDecoder().decode(param.get("keystoreBase64")), param.get("keystorePwd"))
                    .initPfxTrust(Base64.getDecoder().decode(param.get("trustKeyStoreBase64")), param.get("trustKeyStorePwd"));
        }
    },
    JKS {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, Map<String, String> param) {
            return certificate
                    .initJksKey(Base64.getDecoder().decode(param.get("keystoreBase64")), param.get("keystorePwd"))
                    .initJksTrust(Base64.getDecoder().decode(param.get("trustKeyStoreBase64")), param.get("trustKeyStorePwd"));
        }
    },
    PEM {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, Map<String, String> param) {
            List<byte[]> keyCert = Collections.singletonList(Base64.getDecoder().decode(param.get("keystoreBase64")));

            return certificate
                    .initPemKey(keyCert,keyCert)
                    .initPemTrust(Collections.singletonList(Base64.getDecoder().decode(param.get("trustKeyStoreBase64"))));

        }
    };

    public abstract DefaultCertificate init(DefaultCertificate certificate, Map<String, String> param);
}

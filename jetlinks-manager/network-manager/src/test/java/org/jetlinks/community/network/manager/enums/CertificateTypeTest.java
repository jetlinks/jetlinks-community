package org.jetlinks.community.network.manager.enums;

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.K;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.security.DefaultCertificate;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CertificateTypeTest {

    @Test
    void PFX() {
        DefaultCertificate defaultCertificate = new DefaultCertificate("test","test");
        String filePath = this.getClass().getClassLoader().getResource("dlt.p12").getPath();
        String s = readFileContent(filePath);
//        System.out.println(content);
//        String s = content.replaceAll("\t", "");
//        System.out.println(s);

        CertificateEntity.CertificateConfig config = new CertificateEntity.CertificateConfig();
        config.setKeystoreBase64(s);
//        config.setKeystorePwd("ipcc@95598");
        config.setKeystorePwd("ipcc@95598");
        config.setTrustKeyStoreBase64(s);
        config.setTrustKeyStorePwd("ipcc@95598");
        CertificateType.JKS.init(defaultCertificate,config);
    }

    @Test
    void JKS() {

    }

    public static String readFileContent(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }

    @Test
    void valueOf() {
       }
}
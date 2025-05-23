/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.network.security;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.*;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.util.Assert;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 默认证书
 *
 * @author zhouhao
 */
public class DefaultCertificate implements Certificate {

    @Getter
    private final String id;

    @Getter
    private final String name;

    private KeyStoreHelper keyHelper;

    private KeyStoreHelper trustHelper;

    private static final X509Certificate[] EMPTY = new X509Certificate[0];


    public DefaultCertificate(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public DefaultCertificate initPfxKey(byte[] keys, String password) {
        PfxOptions options = new PfxOptions();
        options.setValue(Buffer.buffer(keys));
        options.setPassword(password);
        keyHelper = KeyStoreHelper.create((KeyCertOptions) options);
        return this;
    }

    public DefaultCertificate initPfxTrust(byte[] keys, String password) {
        PfxOptions options = new PfxOptions();
        options.setValue(Buffer.buffer(keys));
        options.setPassword(password);
        trustHelper = KeyStoreHelper.create((TrustOptions) options);
        return this;
    }

    public DefaultCertificate initJksKey(byte[] keys, String password) {
        JksOptions options = new JksOptions();
        options.setValue(Buffer.buffer(keys));
        options.setPassword(password);
        keyHelper = KeyStoreHelper.create((KeyCertOptions) options);
        return this;
    }

    public DefaultCertificate initJksTrust(byte[] keys, String password) {
        JksOptions options = new JksOptions();
        options.setValue(Buffer.buffer(keys));
        options.setPassword(password);
        trustHelper = KeyStoreHelper.create((TrustOptions) options);
        return this;
    }

    public DefaultCertificate initPemKey(List<byte[]> keys, List<byte[]> cert) {
        PemKeyCertOptions options = new PemKeyCertOptions();
        keys.stream().map(Buffer::buffer).forEach(options::addKeyValue);
        cert.stream().map(Buffer::buffer).forEach(options::addCertValue);
        keyHelper = KeyStoreHelper.create(options);
        return this;
    }

    public DefaultCertificate initPemTrust(List<byte[]> cert) {
        PemTrustOptions options = new PemTrustOptions();
        cert.stream().map(Buffer::buffer).forEach(options::addCertValue);
        trustHelper = KeyStoreHelper.create(options);
        return this;
    }

    protected KeyStoreHelper checkKeyStore(KeyStoreHelper helper) {
        Assert.notNull(helper, "key store not init");
        return helper;
    }

    @SneakyThrows
    public DefaultCertificate initKey(KeyStore keyStore, String password) {
        keyHelper = new KeyStoreHelper(keyStore, password);
        return this;
    }

    @SneakyThrows
    public DefaultCertificate initTrust(KeyStore keyStore, String password) {
        trustHelper = new KeyStoreHelper(keyStore, password);
        return this;
    }

    @Override
    @SneakyThrows
    public KeyManagerFactory getKeyManagerFactory() {
        return keyHelper == null ? null : keyHelper.getKeyMgrFactory();
    }

    @Override
    @SneakyThrows
    public TrustManagerFactory getTrustManagerFactory() {
        return trustHelper == null ? null : trustHelper.getTrustMgrFactory();
    }

    @Override
    public X509KeyManager getX509KeyManager(String serverName) {
        return keyHelper == null ? null : keyHelper.getKeyMgr(serverName);
    }

    @Override
    public X509KeyManager[] getX509KeyManagers() {
        return keyHelper == null ? new X509KeyManager[0] : keyHelper.getKeyMgrs();
    }

    @Override
    public X509Certificate[] getCertificateChain(String serverName) {
        return getX509KeyManager(serverName).getCertificateChain(null);
    }

    @Override
    @SneakyThrows
    public X509Certificate[] getTrustCerts() {
        if (trustHelper == null) {
            return EMPTY;
        }
        return Arrays.stream(trustHelper
            .getTrustMgrs())
            .filter(X509TrustManager.class::isInstance)
            .map(X509TrustManager.class::cast)
            .map(X509TrustManager::getAcceptedIssuers)
            .flatMap(Stream::of)
            .toArray(X509Certificate[]::new);
    }

    @Override
    public TrustManager[] getTrustManager(String serverName) {
        return checkKeyStore(trustHelper).getTrustMgr(serverName);
    }
}

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

import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import lombok.SneakyThrows;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.util.function.Function;

public class VertxKeyCertTrustOptions implements KeyCertOptions, TrustOptions {

    private Certificate certificate;

    @SneakyThrows
    public VertxKeyCertTrustOptions(Certificate certificate) {
       this.certificate=certificate;
    }

    @Override
    @SneakyThrows
    public VertxKeyCertTrustOptions clone() {
        return new VertxKeyCertTrustOptions(certificate);
    }

    @Override
    public VertxKeyCertTrustOptions copy() {
        return clone();
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory(Vertx vertx) {
        return certificate.getKeyManagerFactory();
    }

    @Override
    public Function<String, X509KeyManager> keyManagerMapper(Vertx vertx) {
        return certificate::getX509KeyManager;
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
        return certificate.getTrustManagerFactory();
    }

    @Override
    public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) {
        return certificate::getTrustManager;
    }
}

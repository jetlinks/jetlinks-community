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
package org.jetlinks.community.plugin.impl.jar;

import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {

    @Getter
    private final URL[] urls;

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.urls = urls;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            Class<?> clazz = loadSelfClass(name);
            if (null != clazz) {
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        } catch (Throwable ignore) {

        }
        return super.loadClass(name, resolve);
    }

    @SneakyThrows
    public Class<?> loadSelfClass(String name) {
        Class<?> clazz = super.findLoadedClass(name);

        if (clazz != null) {
            return clazz;
        }
        clazz = super.findClass(name);
        resolveClass(clazz);
        return clazz;
    }

    @Override
    public URL getResource(String name) {
        if (StringUtils.isEmpty(name)) {
            return urls[0];
        }
        return super.findResource(name);
    }
}

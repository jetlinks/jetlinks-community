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
package org.jetlinks.community.reactorql.term;

import org.jetlinks.core.metadata.DataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @see org.jetlinks.community.utils.ReactorUtils#createFilter(List)
 */
public class TermTypes {
    private static final Map<String, TermTypeSupport> supports = new LinkedHashMap<>();

    static {
        for (FixedTermTypeSupport value : FixedTermTypeSupport.values()) {
            register(value);
        }
    }

    public static void register(TermTypeSupport support){
        supports.put(support.getType(),support);
    }

    public static List<TermType> lookup(DataType dataType) {

        return supports
            .values()
            .stream()
            .filter(support -> support.isSupported(dataType))
            .map(TermTypeSupport::type)
            .collect(Collectors.toList());
    }

    public static Optional<TermTypeSupport> lookupSupport(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(supports.get(type));
    }
}

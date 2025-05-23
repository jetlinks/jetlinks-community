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
package org.jetlinks.community.reference;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DataReferencedException extends I18nSupportException {

    private final String dataType;
    private final String dataId;

    private final List<DataReferenceInfo> referenceList;


    public DataReferencedException(String dataType,
                                   String dataId,
                                   List<DataReferenceInfo> referenceList) {
        this.dataType = dataType;
        this.dataId = dataId;
        this.referenceList = referenceList;

        super.setI18nCode("error.data.referenced");
    }

    public DataReferencedException(String dataType,
                                   String dataId,
                                   List<DataReferenceInfo> referenceList,
                                   String code,
                                   Object... args) {
        this.dataType = dataType;
        this.dataId = dataId;
        this.referenceList = referenceList;

        super.setI18nCode(code);
        super.setArgs(args);
    }

}

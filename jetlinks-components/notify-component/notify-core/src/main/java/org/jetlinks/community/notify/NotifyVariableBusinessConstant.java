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
package org.jetlinks.community.notify;

/**
 * @author bestfeng
 */
public interface NotifyVariableBusinessConstant {


    String businessId = "businessType";

    interface NotifyVariableBusinessTypes{

        /**
         * 用户
         */
        String userType = "user";

        /**
         * 部门
         */
        String orgType = "org";

        /**
         * 标签
         */
        String tagType = "tag";

        /**
         * 文件
         */
        String fileType = "file";

        /**
         * 链接
         */
        String linkType = "link";
    }

}

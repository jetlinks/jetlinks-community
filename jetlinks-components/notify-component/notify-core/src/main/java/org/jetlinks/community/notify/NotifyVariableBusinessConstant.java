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

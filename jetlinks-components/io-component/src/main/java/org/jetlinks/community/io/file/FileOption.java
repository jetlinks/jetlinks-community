package org.jetlinks.community.io.file;

import org.springframework.util.StringUtils;

public enum FileOption {

    /**
     * 公开访问
     */
    publicAccess,

    /**
     * 临时文件,将会被定时删除
     */
    tempFile;


    public static FileOption[] parse(String str) {
        if (!StringUtils.hasText(str)) {
            return new FileOption[0];
        }

        String[] arr = str.split(",");
        FileOption[] options = new FileOption[arr.length];

        for (int i = 0; i < arr.length; i++) {
            options[i] = FileOption.valueOf(arr[i]);
        }
        return options;
    }
}

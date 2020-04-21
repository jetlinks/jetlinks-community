package org.jetlinks.pro.io.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {


    @Test
    void test(){
        assertEquals(FileUtils.getExtension("test.xlsx"),"xlsx");
        assertEquals(FileUtils.getExtension("test.xlsx?name=abc.a"),"xlsx");
        assertEquals(FileUtils.getExtension("test.xlsx#name=abc.a"),"xlsx");
        assertEquals(FileUtils.getExtension("test.xlsx?id=a.a&name=a#name=abc.a"),"xlsx");

        assertEquals(FileUtils.getExtension("test.xlsx?id=a.a&name=a#name=abc.a"),"xlsx");

    }

}
package org.jetlinks.community.device.service.term;

import org.hswebframework.ezorm.core.meta.Feature;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceTagTermTest {
    public static final String DEVICE_ID = "test001";

    @Test
    void createFragments() {//第一个分支
        DeviceTagTerm deviceTagTerm = new DeviceTagTerm();
        Map<Object, Object> map = new HashMap<>();
        Term term = new Term();
        term.setValue(map);

        //第一实例
        SqlFragments testFragments = deviceTagTerm.createFragments(DEVICE_ID, new RDBColumnMetadata(), term);
        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( 1=2 ) )"
        ,testFragments.toRequest().getSql());
        //第二个实例
        map.put("tag_test","test");
        SqlFragments fragments = deviceTagTerm.createFragments(DEVICE_ID, new RDBColumnMetadata(), term);

//        System.out.println(fragments.getSql().get(1));
//        System.out.println(fragments.toRequest().getSql());
        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( (d.key = ? and d.value like ?) ) )"
            ,fragments.toRequest().getSql());
        Object[] o= {"tag_test","test"};
        assertArrayEquals(o,fragments.toRequest().getParameters());

        //第三个实例
        map.put("tag_test1","test1");
        SqlFragments fragments1 = deviceTagTerm.createFragments(DEVICE_ID, new RDBColumnMetadata(), term);

        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( (d.key = ? and d.value like ?) or (d.key = ? and d.value like ?) ) )"
        ,fragments1.toRequest().getSql());

        Object[] o1= {"tag_test1","test1","tag_test","test"};
        assertArrayEquals(o1,fragments1.toRequest().getParameters());
    }

    @Test
    void createFragments1() {//第二个分支
        DeviceTagTerm deviceTagTerm = new DeviceTagTerm();
        List<Map<String, String>> list = new ArrayList<>();
        Term term = new Term();
        term.setValue(list);
        SqlFragments fragments = deviceTagTerm.createFragments(DEVICE_ID, new RDBColumnMetadata(), term);
        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( 1=2 ) )"
            ,fragments.toRequest().getSql());
    }
    @Test
    void createFragments2() {//第三个分支
        DeviceTagTerm deviceTagTerm = new DeviceTagTerm();
        String s = "{'tags':'test'}";
        Term term = new Term();
        term.setValue(s);
        //实例一
        SqlFragments fragments = deviceTagTerm.createFragments(DEVICE_ID, new RDBColumnMetadata(), term);
        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( (d.key = ? and d.value like ?) ) )"
            ,fragments.toRequest().getSql());
        Object[] o = {"tags","test"};
        assertArrayEquals(o,fragments.toRequest().getParameters());

        //实例二
        String s1 ="[{'key':'test','value':'test'}]";
        term.setValue(s1);
        SqlFragments fragments1 = deviceTagTerm.createFragments(DEVICE_ID, new RDBColumnMetadata(), term);
        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( (d.key = ? and d.value like ?) ) )"
            ,fragments1.toRequest().getSql());
        Object[] o1 = {"test","test"};
        assertArrayEquals(o1,fragments1.toRequest().getParameters());

        //实例三
        String s2 ="test:test,test1:test1";
        term.setValue(s2);
        SqlFragments fragments2 = deviceTagTerm.createFragments(DEVICE_ID, new RDBColumnMetadata(), term);
        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( (d.key = ? and d.value like ?) or (d.key = ? and d.value like ?) ) )"
            ,fragments2.toRequest().getSql());
//        for (Object parameter : fragments2.toRequest().getParameters()) {
//
//            System.out.println(parameter);
//        }
        Object[] o2 = {"test","test","test1","test1"};
        assertArrayEquals(o2,fragments2.toRequest().getParameters());

        String s3 ="select 1 from dev_device_tags d where d.device_id = test001";
        term.setValue(s3);
        RDBColumnMetadata rdbColumnMetadata = new RDBColumnMetadata();
        rdbColumnMetadata.setDataType("varchar(32)");
        Map<String, Feature> map = new HashMap<>();
        map.put("termType:1",new DeviceTagTerm());
        map.put("termType:d",new DeviceTagTerm());
        map.put("termType:eq",new DeviceTagTerm());
        rdbColumnMetadata.setFeatures(map);
        SqlFragments fragments3 = deviceTagTerm.createFragments(DEVICE_ID, rdbColumnMetadata, term);
        assertEquals("exists(select 1 from dev_device_tags d where d.device_id = test001 and ( (d.key = ? and exists(select 1 from dev_device_tags d where d.device_id = d.value and ( ) ) ) and (d.key = ? and exists(select 1 from dev_device_tags d where d.device_id = d.value and ( ) ) ) and (d.key = ? and exists(select 1 from dev_device_tags d where d.device_id = d.value and ( ) ) ) ) )"
            ,fragments3.toRequest().getSql());
    }
}
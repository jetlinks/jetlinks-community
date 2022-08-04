import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.entity.TestEntity;
import org.jetlinks.community.rule.engine.service.DeviceAlarmService;
import org.jetlinks.community.rule.engine.service.TestService;
import org.jetlinks.community.standalone.JetLinksApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.atomic.AtomicReference;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {JetLinksApplication.class})
public class test2 {
    @Autowired
    TestService testService;
    @Autowired
    DeviceAlarmService deviceAlarmService;
    @Test
    public void test(){
//        TestEntity testEntity = testService.findById("${MV}").block();
//
//        Double sValue = testEntity.getSValue();
//
//        System.out.println(sValue);

        DeviceAlarmEntity block = deviceAlarmService.findById("1544879707789897728").block();

        System.out.println("***************************");
        System.out.println(block.toString());
        System.out.println("***************************");




    }
}

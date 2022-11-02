package org.jetlinks.community.device.web;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.FloatType;
import org.jetlinks.community.PropertyMetadataConstants;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.web.request.AggRequest;
import org.jetlinks.community.relation.entity.RelationEntity;
import org.jetlinks.community.relation.service.RelatedObjectInfo;
import org.jetlinks.community.relation.service.RelationService;
import org.jetlinks.community.relation.service.request.SaveRelationRequest;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(value = DeviceInstanceController.class, properties = {
    "spring.reactor.debug-agent.enabled=true"
})
class DeviceInstanceControllerTest extends TestJetLinksController {

    @Autowired
    @SuppressWarnings("all")
    private LocalDeviceInstanceService deviceService;

    @Autowired
    @SuppressWarnings("all")
    private LocalDeviceProductService productService;

    private String deviceId;
    private String productId;

    private String metadata;
    @BeforeEach
    void setup() {
        DeviceProductEntity product = new DeviceProductEntity();
        product.setMetadata("{}");
        product.setTransportProtocol("MQTT");
        product.setMessageProtocol("test");
        product.setId(productId = "deviceinstancecontrollertest_product");
        product.setName("DeviceInstanceControllerTest");

        JetLinksDeviceMetadata metadata = new JetLinksDeviceMetadata("Test", "Test");
        {
            SimplePropertyMetadata metric = SimplePropertyMetadata.of(
                "metric", "Metric", FloatType.GLOBAL
            );
            metric.setExpands(
                PropertyMetadataConstants.Metrics
                    .metricsToExpands(Arrays.asList(
                        PropertyMetric.of("max", "最大值", 100),
                        PropertyMetric.of("min", "最小值", -100)
                    ))
            );
            metadata.addProperty(metric);
        }

        product.setMetadata(this.metadata=JetLinksDeviceMetadataCodec.getInstance().doEncode(metadata));

        productService
            .save(product)
            .then(productService.deploy(productId))
            .then()
            .as(StepVerifier::create)
            .expectComplete()
            .verify();

        DeviceInstanceEntity device = new DeviceInstanceEntity();
        device.setId(deviceId = "deviceinstancecontrollertest_device");
        device.setName("DeviceInstanceControllerTest");
        device.setProductId(product.getId());
        device.setProductName(device.getName());

        client
            .patch()
            .uri("/device/instance")
            .bodyValue(device)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .put()
            .uri("/device/instance/batch/_deploy")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Arrays.asList(deviceId))
            .exchange()
            .expectStatus()
            .is2xxSuccessful();


    }

    @AfterEach
    void shutdown() {
        client
            .put()
            .uri("/device/instance/batch/_unDeploy")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Arrays.asList(deviceId))
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .put()
            .uri("/device/instance/batch/_delete")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Arrays.asList(deviceId))
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @SneakyThrows
    void testCommon() {
//        {
//            DeviceInstanceEntity device = new DeviceInstanceEntity();
//            device.setId(deviceId);
//            device.setName("DeviceInstanceControllerTest");
//            device.setProductId(productId);
//            device.setProductName(device.getName());
//            //重复创建
//            client
//                .post()
//                .uri("/device/instance")
//                .bodyValue(device)
//                .exchange()
//                .expectStatus()
//                .is4xxClientError();
//        }
        client
            .get()
            .uri("/device/instance/{id:.+}/detail", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri("/device/instance/bind-providers")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();


        client
            .get()
            .uri("/device/instance/{deviceId}/state", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
        client
            .get()
            .uri("/device/instance/state/_sync")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();


        client
            .post()
            .uri("/device/instance/{deviceId}/deploy", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();


        client
            .post()
            .uri("/device/instance/{deviceId}/undeploy", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .post()
            .uri("/device/instance/{deviceId}/disconnect", deviceId)
            .exchange();
    }

    @Test
    void testProperties() {

        client
            .get()
            .uri("/device/instance/{deviceId:.+}/properties/_query?where=property is test", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri("/device/instance/{deviceId:.+}/properties/_query", deviceId)
            .exchange()
            .expectStatus()
            .is4xxClientError();

        client
            .get()
            .uri("/device/instance/{deviceId:.+}/properties/latest", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri("/device/instance/{deviceId:.+}/properties", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .post()
            .uri("/device/instance/{deviceId:.+}/properties/_top/{numberOfTop}", deviceId, 1)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri("/device/instance/{deviceId:.+}/property/{property}/_query", deviceId, "test")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .post()
            .uri("/device/instance/{deviceId:.+}/property/{property}/_query", deviceId, "test")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .post()
            .uri("/device/instance/{deviceId:.+}/properties/_query", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange();


        client
            .get()
            .uri("/device/instance/{deviceId:.+}/property/{property:.+}", deviceId, "test")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        AggRequest request = new AggRequest();

        request.setColumns(Arrays.asList(
            new DeviceDataService.DevicePropertyAggregation("test", "alias", Aggregation.AVG)
        ));
        request.setQuery(DeviceDataService.AggregationRequest
                             .builder()
                             .build());

        client
            .post()
            .uri("/device/instance/{deviceId:.+}/agg/_query", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange();


    }

    @Test
    void testEvent() {
        client
            .get()
            .uri("/device/instance/{deviceId:.+}/event/{eventId}", deviceId, "test")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
        client
            .post()
            .uri("/device/instance/{deviceId:.+}/event/{eventId}", deviceId, "test")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void testLog() {
        client
            .get()
            .uri("/device/instance/{deviceId:.+}/logs", deviceId, "test")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
        client
            .post()
            .uri("/device/instance/{deviceId:.+}/logs", deviceId, "test")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void testTag() {
        DeviceTagEntity tag = new DeviceTagEntity();
        tag.setKey("test");
        tag.setValue("value");

        client
            .patch()
            .uri("/device/instance/{deviceId}/tag", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(tag)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        List<DeviceTagEntity> tags = client
            .get()
            .uri("/device/instance/{deviceId}/tags", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceTagEntity.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(tags);
        assertFalse(tags.isEmpty());

        client
            .delete()
            .uri("/device/instance/{deviceId}/tag/{tagId}", deviceId, tags.get(0).getId())
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @SneakyThrows
    void testMetadata() {
        client
            .get()
            .uri("/device/instance/{id:.+}/config-metadata", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri("/device/instance/{id:.+}/config-metadata/{metadataType}/{metadataId}/{typeId}",
                 deviceId,
                 "property",
                 "temp",
                 "test")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();


        String metadata = client
            .post()
            .uri("/device/instance/{deviceId}/property-metadata/import?fileUrl=" + new ClassPathResource("property-metadata.csv")
                .getFile()
                .getAbsolutePath(), deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(metadata);
        client
            .put()
            .uri("/device/instance/{id:.+}/metadata", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(metadata)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        deviceService.findById(deviceId)
                     .as(StepVerifier::create)
                     .expectNextMatches(device -> Objects.equals(
                         device.getDeriveMetadata(),
                         metadata
                     ))
                     .expectComplete()
                     .verify();
        client
            .put()
            .uri("/device/instance/{id}/metadata/merge-product", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .delete()
            .uri("/device/instance/{id}/metadata", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        deviceService
            .findById(deviceId)
            .as(StepVerifier::create)
            .expectNextMatches(device -> StringUtils.isEmpty(device.getDeriveMetadata()))
            .expectComplete()
            .verify();
    }

    @Test
    void testConfiguration() {

        deviceService.deploy(deviceId)
                     .then()
                     .as(StepVerifier::create)
                     .expectComplete()
                     .verify();
        client
            .post()
            .uri("/device/instance/{deviceId:.+}/configuration/_write", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"test\":\"123\"}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .post()
            .uri("/device/instance/{deviceId:.+}/configuration/_read", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("[\"test\"]")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Map.class)
            .isEqualTo(Collections.singletonMap("test", "123"));

        client
            .put()
            .uri("/device/instance/{deviceId:.+}/configuration/_reset", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .put()
            .uri("/device/instance/{deviceId:.+}/shadow", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"test\":\"123\"}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri("/device/instance/{deviceId:.+}/shadow", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .isEqualTo("{\"test\":\"123\"}");

    }

    @Test
    void testCommand() {
        client
            .put()
            .uri("/device/instance/{deviceId:.+}/property", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Collections.singletonMap("test", "value"))
            .exchange();

        client
            .post()
            .uri("/device/instance/{deviceId:.+}/property/_read", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Collections.singleton("test"))
            .exchange();


        client
            .post()
            .uri("/device/instance/{deviceId:.+}/function/test", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Collections.singletonMap("test", "value"))
            .exchange();

        client
            .post()
            .uri("/device/instance/{deviceId:.+}/message", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Collections.singletonMap("properties", Collections.singletonList("test")))
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", "test");
        data.put("properties", Collections.singletonList("test"));

        client
            .post()
            .uri("/device/instance/messages", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(data)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    void testAutoChangeProductInfo() {
        client.post()
              .uri("/device/instance")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue("{\"id\":\"testAutoChangeProductInfo\",\"name\":\"Test\",\"productId\":\"" + productId + "\"}")
              .exchange()
              .expectStatus()
              .is2xxSuccessful();

    }

    @Autowired
    private RelationService relationService;

    @Test
    void testRelation() {
        RelationEntity entity = new RelationEntity();
        entity.setRelation("manager");
        entity.setObjectType("device");
        entity.setObjectTypeName("设备");
        entity.setTargetType("user");
        entity.setTargetTypeName("用户");
        entity.setName("管理员");
        relationService
            .save(entity).block();


        client.get()
              .uri("/device/instance/{deviceId}/detail", deviceId)
              .exchange()
              .expectStatus()
              .is2xxSuccessful()
              .expectBody()
              .jsonPath("$.relations[0].relation").isEqualTo("manager")
              .jsonPath("$.relations[0].related").isEmpty();


        client.patch()
              .uri("/device/instance/{deviceId}/relations", deviceId)
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(SaveRelationRequest.of(
                  "user",
                  "manager",
                  Arrays.asList(RelatedObjectInfo.of("admin", "管理员")),
                  null
              ))
              .exchange()
              .expectStatus()
              .is2xxSuccessful();

        client.get()
              .uri("/device/instance/{deviceId}/detail", deviceId)
              .exchange()
              .expectStatus()
              .is2xxSuccessful()
              .expectBody()
              .jsonPath("$.relations[0].relation").isEqualTo("manager")
              .jsonPath("$.relations[0].related[0].id").isEqualTo("admin")
              .jsonPath("$.relations[0].related[0].name").isEqualTo("管理员");


    }

    @Test
    void testMetric() {
        client.get()
              .uri("/device/instance/{deviceId}/metric/property/{property}", deviceId, "metric")
              .exchange()
              .expectStatus()
              .is2xxSuccessful()
              .expectBody()
              .jsonPath("[0].id").isEqualTo("max")
              .jsonPath("[0].value").isEqualTo(100)
              .jsonPath("[1].id").isEqualTo("min")
              .jsonPath("[1].value").isEqualTo(-100);


        client.patch()
              .uri("/device/instance/{deviceId}/metric/property/{property}", deviceId, "metric")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(PropertyMetric.of("max", "最大值", 110))
              .exchange()
              .expectStatus()
              .is2xxSuccessful();

        client.get()
              .uri("/device/instance/{deviceId}/metric/property/{property}", deviceId, "metric")
              .exchange()
              .expectStatus()
              .is2xxSuccessful()
              .expectBody()
              .jsonPath("[0].id").isEqualTo("max")
              .jsonPath("[0].value").isEqualTo(110)
              .jsonPath("[1].id").isEqualTo("min")
              .jsonPath("[1].value").isEqualTo(-100);


    }

    @Test
    void testDetail(){
        client
            .get()
            .uri("/device/instance/{deviceId}/detail", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.metadata").isEqualTo(metadata);

        String newMetadata="{\"properties\":[]}";

        client
            .put()
            .uri("/device/product/{productId}", productId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"metadata\":"+ JSON.toJSONString(newMetadata)+"}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        //仅保存 未发布
        client
            .get()
            .uri("/device/instance/{deviceId}/detail", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.metadata").isEqualTo(metadata);

        client
            .post()
            .uri("/device/product/{productId}/deploy", productId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri("/device/instance/{deviceId}/detail", deviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.metadata").isEqualTo(newMetadata);


    }
}
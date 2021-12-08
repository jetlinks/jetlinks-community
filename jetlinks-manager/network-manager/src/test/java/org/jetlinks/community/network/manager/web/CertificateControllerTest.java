package org.jetlinks.community.network.manager.web;


import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.File;


@WebFluxTest(CertificateController.class)
class CertificateControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/network/certificate";
    private static final String ID = "test";

    @Test
    void getService() {
        new CertificateController().getService();
    }

    @Test
    void getCertificateInfo() {
        client.get()
            .uri(BASE_URL+"/"+ID+"/detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void upload() {
        String filePath = this.getClass().getClassLoader().getResource("changgou.pem").getPath();
        // 封装请求参数
        FileSystemResource resource = new FileSystemResource(new File(filePath));
        MultiValueMap<String, Object> param = new LinkedMultiValueMap<>();
        param.add("file", resource);  //服务端MultipartFile uploadFile

        String body = client.post()
            .uri(BASE_URL + "/upload")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromMultipartData(param))
            //.body("C:\\web\\blog\\blog-parent\\blog-user-oauth\\src\\main\\resources\\blog.jks")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        System.out.println(body);
    }

    @Test
    void upload1(){
        MultiValueMap<String, Object> param = new LinkedMultiValueMap<>();
        param.add("file","aa");
        client.post()
            .uri(BASE_URL+"/upload")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromMultipartData(param))
            //.body("C:\\web\\blog\\blog-parent\\blog-user-oauth\\src\\main\\resources\\blog.jks")
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }
}
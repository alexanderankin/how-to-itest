package org.example.itest.fs.copy.per;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.images.builder.Transferable;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT24H")
class FsCopyPerTest {
    static final String CONTAINER_INDEX_HTML = "/usr/share/nginx/html/index.html";
    static NginxContainer<?> container;

    @Autowired
    WebTestClient webTestClient;

    @Value("${external-url}")
    String baseUrl;

    @BeforeAll
    static void setUp() {
        // noinspection resource
        container = new NginxContainer<>("nginx")
                .withCopyToContainer(
                        Transferable.of("<h1>It works!</h1>"),
                        CONTAINER_INDEX_HTML);
        container.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("external-url",
                () -> UriComponentsBuilder.newInstance()
                        .scheme("http")
                        .host("localhost")
                        .port(container.getMappedPort(80))
                        .build().toString());
    }

    @Test
    void test_one() {
        container.copyFileToContainer(Transferable.of("test_one"), CONTAINER_INDEX_HTML);
        webTestClient.get().uri(baseUrl).exchange().expectBody(String.class).value(Matchers.is("test_one"));
    }

    @Test
    void test_two() {
        container.copyFileToContainer(Transferable.of("test_two"), CONTAINER_INDEX_HTML);
        webTestClient.get().uri(baseUrl).exchange().expectBody(String.class).value(Matchers.is("test_two"));
    }

}

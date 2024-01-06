package org.example;

import com.github.dockerjava.api.model.HostConfig;
import io.milvus.client.MilvusServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpStatus.OK;

@SuppressWarnings("resource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MilvusSampleAppITest {
    static Network NETWORK = Network.newNetwork();

    // https://milvus.io/docs/prerequisite-docker.md#Software-requirements
    static GenericContainer<?> ETCD_CONTAINER =
            new GenericContainer<>("bitnami/etcd:3.5.0")
                    .withNetwork(NETWORK)
                    .withEnv("ALLOW_NONE_AUTHENTICATION", "yes")
                    .withExposedPorts(2379)
                    .withNetworkAliases("etcd");
    static GenericContainer<?> MINIO_CONTAINER =
            new GenericContainer<>("minio/minio:RELEASE.2023-03-20T20-16-18Z")
                    .withNetwork(NETWORK)
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withNetworkAliases("minio")
                    .withExposedPorts(9000, 9001)
                    .withCommand("minio server /minio_data --console-address :9001");
    static GenericContainer<?> MILVUS_CONTAINER =
            new GenericContainer<>("milvusdb/milvus:v2.3.3")
                    .withNetwork(NETWORK)
                    .withNetworkAliases("milvus")
                    .withExposedPorts(9091, 19530)
                    .dependsOn(ETCD_CONTAINER, MINIO_CONTAINER)
                    .withEnv("ETCD_ENDPOINTS", "etcd:2379")
                    .withEnv("MINIO_ADDRESS", "minio:9000")
                    .withEnv("MINIO_ACCESS_KEY_ID", "minioadmin")
                    .withEnv("MINIO_SECRET_ACCESS_KEY", "minioadmin")
                    .waitingFor(Wait.forHttp("/healthz").forPort(9091))
                    .withCommand("milvus", "run", "standalone")
                    .withCreateContainerCmdModifier(c ->
                            c.withHostConfig(Objects.requireNonNullElseGet(c.getHostConfig(), HostConfig::new)
                                    .withSecurityOpts(List.of("seccomp:unconfined"))));

    @Autowired
    MilvusServiceClient milvusClient;

    @Autowired
    TestRestTemplate testRestTemplate;

    @DynamicPropertySource
    static void milvusPropertySource(DynamicPropertyRegistry registry) {
        MILVUS_CONTAINER.start();
        registry.add("milvus.address",
                () -> MILVUS_CONTAINER.getHost() + ":" +
                      MILVUS_CONTAINER.getMappedPort(19530));
    }

    @Test
    void test_client() {
        // haven't figured this out yet
        assertThat(milvusClient.checkHealth().getException(), is(notNullValue()));
    }

    @Test
    void test_healthEndpoint() {
        URI healthCheckUrl = UriComponentsBuilder.fromPath("/healthz")
                .scheme("http")
                .host(MILVUS_CONTAINER.getHost())
                .port(MILVUS_CONTAINER.getMappedPort(9091))
                .build()
                .toUri();

        ResponseEntity<String> exchange =
                testRestTemplate.exchange(RequestEntity.get(healthCheckUrl).build(), String.class);

        assertThat(exchange, is(notNullValue()));
        assertThat(exchange.getStatusCode(), is(OK));
        assertThat(exchange.getBody(), is("OK"));
    }
}

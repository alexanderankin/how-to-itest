package org.example.itest.db.postgres.r2dbc;

import lombok.extern.slf4j.Slf4j;
import org.example.itest.db.postgres.r2dbc.R2dbcSampleApp.Examples.Example;
import org.example.itest.db.postgres.r2dbc.R2dbcSampleApp.Examples.ExampleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ActiveProfiles({"test"})
@AutoConfigureWebTestClient
// @Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @TestPropertySource(properties = {
//     "spring.r2dbc.url=r2dbc:tc:postgresql:///testing-db?TC_IMAGE_TAG=15-alpine",
//     "spring.r2dbc.username=postgres",
//     "spring.r2dbc.password=secret"
// })
@Slf4j
class R2dbcSampleAppITest {

    // @Container
    // @ServiceConnection
    static PostgreSQLContainer<?> postgreSqlContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            // .withCopyFileToContainer(MountableFile.forHostPath("001-ddl.sql"), "/docker-entrypoint-initdb.d/001-ddl.sql")
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withNetworkAliases("postgres")
            .withDatabaseName("testing-db")
            .withUsername("postgres")
            .withPassword("secret")
            .withExposedPorts(5432);

    @Autowired
    WebTestClient webTestClient;
    @Autowired
    ExampleRepository exampleRepository;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        postgreSqlContainer.start();
        registry.add("spring.r2dbc.url", () -> postgreSqlContainer.getJdbcUrl()
                .replace("jdbc", "r2dbc"));
        registry.add("spring.r2dbc.username", postgreSqlContainer::getUsername);
        registry.add("spring.r2dbc.password", postgreSqlContainer::getPassword);
    }

    /*
    @Test
    void get_hello() {
        webTestClient.get()
            .uri("/hello")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
    */

    @Test
    void examples() {
        Example example = exampleRepository.save(new Example().setName("name")).block();
        assertThat(example, is(not(nullValue())));

        Example foundById = exampleRepository.findById(example.getId()).block();
        assertThat(foundById, is(not(nullValue())));
        assertThat(example.getName(), is(equalTo(foundById.getName())));
    }
}

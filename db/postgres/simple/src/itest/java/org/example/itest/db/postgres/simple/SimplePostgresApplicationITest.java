package org.example.itest.db.postgres.simple;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.RequestEntity;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT24H") // allow debugging server code
@ContextConfiguration(initializers = SimplePostgresApplicationITest.Initializer.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SimplePostgresApplicationITest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TestRestTemplate testRestTemplate;

    @LocalServerPort
    Integer port;

    @SuppressWarnings("CommentedOutCode")
    @Test
    void test_getAll() {
        webTestClient.post().uri("/examples").bodyValue(new SimplePostgresApplication.Example().setName("abc"))
                .exchange()
                .expectStatus().is2xxSuccessful();

        /*
        // todo determine why webClient is not configured here
        PagedModel<SimplePostgresApplication.Example> responseBody = webTestClient.get().uri("/examples").exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedModel<SimplePostgresApplication.Example>>() {
                })
                .returnResult().getResponseBody();

        assertThat(responseBody, is(notNullValue()));
        // assertThat(responseBody.getContent(), hasSize(0));
        */

        var response = testRestTemplate.exchange(
                RequestEntity.get("/examples").build(),
                new ParameterizedTypeReference<PagedModel<SimplePostgresApplication.Example>>() {
                });


        assertThat(response.getBody(), is(notNullValue()));
        assertThat(response.getBody().getContent(), hasSize(1));
        assertThat(response.getBody().getContent().stream().findAny().orElseThrow().getName(), is("abc"));
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
            PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14-alpine");
            postgreSQLContainer.start();

            applicationContext.getBeanFactory()
                    .registerSingleton("postgreSQLContainer", postgreSQLContainer);

            TestPropertyValues.of(Map.ofEntries(
                    Map.entry("spring.datasource.url", postgreSQLContainer.getJdbcUrl()),
                    Map.entry("spring.datasource.username", postgreSQLContainer.getUsername()),
                    Map.entry("spring.datasource.password", postgreSQLContainer.getPassword())
            )).applyTo(applicationContext.getEnvironment());
        }
    }
}

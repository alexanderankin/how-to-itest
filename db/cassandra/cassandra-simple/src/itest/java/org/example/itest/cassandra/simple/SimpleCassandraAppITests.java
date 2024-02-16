package org.example.itest.cassandra.simple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.CassandraContainer;

import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT24H")
@ContextConfiguration(initializers = SimpleCassandraAppITests.Init.class)
public class SimpleCassandraAppITests {

    static final ParameterizedTypeReference<SimpleCassandraApp.CassandraPage<SimpleCassandraApp.Example>> PAGE_OF_EXAMPLES = new ParameterizedTypeReference<>() {
    };

    @Autowired
    WebTestClient webTestClient;

    @Test
    void test() {
        webTestClient.get().uri("/api/example").exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_OF_EXAMPLES)
                .value(SimpleCassandraApp.CassandraPage::elements, is(empty()));

        webTestClient.post().uri("/api/example")
                .bodyValue(new SimpleCassandraApp.Example().setName("SimpleCassandraAppITests"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SimpleCassandraApp.Example.class)
                .value(SimpleCassandraApp.Example::getSource, is(notNullValue()))
                .value(SimpleCassandraApp.Example::getId, is(notNullValue()))
                .value(SimpleCassandraApp.Example::getCreatedAt, is(notNullValue()))
                .value(SimpleCassandraApp.Example::getName, is(notNullValue()));

        webTestClient.get().uri("/api/example").exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_OF_EXAMPLES)
                .value(SimpleCassandraApp.CassandraPage::elements, is(not(empty())))
                .value(SimpleCassandraApp.CassandraPage::elements, hasSize(1));

        for (int i = 0; i < 10; i++)
            webTestClient.post().uri("/api/example")
                    .bodyValue(new SimpleCassandraApp.Example()
                            .setName("SimpleCassandraAppITests." + i))
                    .exchange();

        webTestClient.get().uri("/api/example").exchange()
                .expectBody(PAGE_OF_EXAMPLES).value(SimpleCassandraApp.CassandraPage::pagingState, is(blankOrNullString()));

        webTestClient.get().uri("/api/example?size=1").exchange()
                .expectBody(PAGE_OF_EXAMPLES).value(SimpleCassandraApp.CassandraPage::pagingState, is(not(blankOrNullString())));
    }

    static class Init implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SuppressWarnings("resource")
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
            var c = new CassandraContainer<>("cassandra:4");
            c.start();

            TestPropertyValues.of(
                    "spring.cassandra.contact-points=" + c.getHost() + ":" + c.getContactPoint().getPort(),
                    "spring.cassandra.local-datacenter=" + c.getLocalDatacenter()
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}

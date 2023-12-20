package org.example.itest.db.postgres.multidb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT24H") // so you can debug server side code
@ContextConfiguration(classes = TestMultiDbApp.TestConfig.class)
public class MultiDbAppITest {
    public static final ParameterizedTypeReference<List<String>> LIST_OF_STRINGS = new ParameterizedTypeReference<>() {
    };
    @Autowired
    WebTestClient webTestClient;

    @Test
    void canRequestValidDbs() {
        for (String db : List.of("db1", "db2")) {
            webTestClient.get().uri("/{db}/items", db).exchange()
                    .expectStatus().is2xxSuccessful()
                    .expectBody(new ParameterizedTypeReference<List<String>>() {
                    })
                    .isEqualTo(List.of());

        }
    }

    @Test
    void cannotRequestInvalidDbs() {
        webTestClient.get().uri("/db12345/items").exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void writesToEvenDbsDoNotAppearInOddDbs() {
        String prefix = "writesToEvenDbsDoNotAppearInOddDbs";
        webTestClient.get().uri("/db2/items").exchange().expectBody(LIST_OF_STRINGS).value(not(hasItem(prefix + ".2")));

        webTestClient.post().uri("/{db}/items", "db2").bodyValue(Map.of("item", prefix + ".2"))
                .exchange().expectStatus().isCreated();

        webTestClient.get().uri("/db1/items").exchange().expectBody(LIST_OF_STRINGS).value(not(hasItem(prefix + ".2")));
        webTestClient.get().uri("/db2/items").exchange().expectBody(LIST_OF_STRINGS).value(hasItem(prefix + ".2"));
        webTestClient.get().uri("/db3/items").exchange().expectBody(LIST_OF_STRINGS).value(not(hasItem(prefix + ".2")));
    }

    @Test
    void writesToAllDbsGetReflectedInTheirRespectiveDb() {
        for (String db : List.of("db1", "db2", "db3", "db4")) {
            for (String item : List.of("abc", "def", "ghi")) {
                String dbItem = db + "." + item;

                webTestClient.get().uri("/{db}/items", db).exchange().expectBody(LIST_OF_STRINGS).value(not(hasItem(dbItem)));

                webTestClient.post().uri("/{db}/items", db)
                        .bodyValue(Map.of("item", dbItem))
                        .exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .value(m -> assertThat(m.get("item"), is(dbItem)))
                        .value(m -> assertThat(m.containsKey("id"), is(true)));

                webTestClient.get().uri("/{db}/items", db).exchange().expectBody(LIST_OF_STRINGS).value(hasItem(dbItem));
            }
        }
    }
}

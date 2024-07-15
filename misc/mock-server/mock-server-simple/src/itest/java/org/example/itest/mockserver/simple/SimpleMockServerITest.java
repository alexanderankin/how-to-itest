package org.example.itest.mockserver.simple;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

public class SimpleMockServerITest {
    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
            .parse("mockserver/mockserver")
            .withTag("mockserver-5.14.0");


    static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE) {{
        addFixedExposedPort(PORT, PORT);
    }};

    ObjectMapper objectMapper;
    MockServerClient localhost;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockServer.start();
        localhost = new MockServerClient("localhost", mockServer.getServerPort());
    }

    @Test
    void test() {
        mockServer.getEndpoint();

        localhost
                .when(
                        request().withPath("/some/path"),
                        Times.exactly(1)
                )
                .respond(request -> response().withBody(stringify(Map.of("id", 1))));

        localhost
                .when(request().withPath("/some/path"))
                .respond(
                        request -> {
                            if (request.hasQueryStringParameter("includeDeleted", "true")) {
                                return response()
                                        .withStatusCode(OK_200.code())
                                        .withBody(stringify(Map.of("id", 1)));
                            } else {
                                return notFoundResponse();
                            }
                        }
                );

        WebClient webClient = WebClient.builder().baseUrl(mockServer.getEndpoint()).build();
        assertThat(webClient.get().uri("/some/path").exchangeToMono(e -> e.toEntity(String.class)).blockOptional().orElseThrow().getStatusCode().is2xxSuccessful(), is(true));
        assertThat(webClient.get().uri("/some/path").exchangeToMono(e -> e.toEntity(String.class)).blockOptional().orElseThrow().getStatusCode().is2xxSuccessful(), is(false));
        assertThat(webClient.get().uri("/some/path?includeDeleted=true").exchangeToMono(e -> e.toEntity(String.class)).blockOptional().orElseThrow().getStatusCode().is2xxSuccessful(), is(true));
    }

    @SneakyThrows
    private String stringify(Object o) {
        return objectMapper.writeValueAsString(o);
    }
}

package org.example.itest.aws.sqs_sns.queue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT24H") // server side debugging
@ContextConfiguration(initializers = SqsSnsQueueAppITest.Init.class)
class SqsSnsQueueAppITest {
    @Autowired
    WebTestClient webTestClient;

    @Test
    void test() {
        {
            var result = webTestClient.get().uri("/messages").exchange().expectBody(String.class).returnResult();
            System.out.println(result);
        }
        System.out.println("-----");
        webTestClient.post().uri("/messages").bodyValue("hello world").exchange();
        {
            var result = webTestClient.get().uri("/messages").exchange().expectBody(String.class).returnResult();
            System.out.println(result);
        }
    }

    static class Init implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext ctx) {
            var localstackImage = DockerImageName.parse("localstack/localstack:3.3.0");
            var localStack = new LocalStackContainer(localstackImage)
                    .withServices(LocalStackContainer.Service.SQS,
                            LocalStackContainer.Service.STS,
                            LocalStackContainer.Service.SNS);
            localStack.start();

            TestPropertyValues.of(
                    "spring.cloud.aws.credentials.access-key=" + localStack.getAccessKey(),
                    "spring.cloud.aws.credentials.secret-key=" + localStack.getSecretKey(),
                    "spring.cloud.aws.endpoint=" + localStack.getEndpoint(),
                    "spring.cloud.aws.sqs.endpoint=" + localStack.getEndpointOverride(LocalStackContainer.Service.SQS),
                    "spring.cloud.aws.sqs.region=" + localStack.getRegion()
            ).applyTo(ctx.getEnvironment());
        }
    }
}

package org.example.itest.aws.sqs.simple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = SimpleSqsAppITest.Init.class)
class SimpleSqsAppITest {

    @Autowired
    SimpleSqsApp.Comp component;

    @Test
    void test() {
        component.send("hello");
        Optional<Message<String>> message = component.get();
        System.out.println(message);
        assertThat(message.isPresent(), is(true));
    }

    static class Init implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext ctx) {
            LocalStackContainer l = new LocalStackContainer(
                    DockerImageName.parse("localstack/localstack")
                            .withTag("3.3.0")
            )
                    .withServices(LocalStackContainer.Service.SQS);
            l.start();

            TestPropertyValues.of(
                    "spring.cloud.aws.credentials.access-key=" + l.getAccessKey(),
                    "spring.cloud.aws.credentials.secret-key=" + l.getSecretKey(),
                    "spring.cloud.aws.sqs.endpoint=" + l.getEndpointOverride(LocalStackContainer.Service.SQS),
                    "spring.cloud.aws.region.static=" + l.getRegion()
            ).applyTo(ctx.getEnvironment());
        }
    }
}

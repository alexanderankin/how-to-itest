package org.example.itest.ddb.sdk;

import org.example.itest.ddb.sdk.DynamoDbSdkExampleApp.User;
import org.example.itest.ddb.sdk.DynamoDbSdkExampleApp.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@ContextConfiguration(initializers = DynamoDbSdkExampleAppTest.Init.class)
class DynamoDbSdkExampleAppTest {
    @Autowired
    UserRepo userRepo;

    @Test
    void test() {
        User abc = userRepo.save(User.builder().id(UUID.randomUUID()).name("abc").build());
        User byId = userRepo.findById(abc.getId());
        assertThat(byId, is(equalTo(abc)));
    }

    static class Init implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext ctx) {
            DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:3.3.0");

            var localstack = new LocalStackContainer(localstackImage)
                    .withServices(LocalStackContainer.Service.DYNAMODB);
            localstack.start();

            // realized I could do it this way instead
            // probably for the best in terms of parity with real env
            System.setProperty("aws.accessKeyId", localstack.getAccessKey());
            System.setProperty("aws.secretAccessKey", localstack.getSecretKey());
            System.setProperty("aws.region", localstack.getRegion());

            // unavoidable as far as I can tell
            TestPropertyValues.of("ddb.endpoint=" + localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                    .applyTo(ctx.getEnvironment());
        }
    }
}

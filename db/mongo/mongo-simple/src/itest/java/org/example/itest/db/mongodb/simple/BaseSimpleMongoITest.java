package org.example.itest.db.mongodb.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = SimpleMongoAppITest.Init.class)
public abstract class BaseSimpleMongoITest {
    @Autowired
    protected MongoTemplate mongoTemplate;
    @Autowired
    protected TestRestTemplate testRestTemplate;

    static class Init implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext ctx) {
            MongoDBContainer m = new MongoDBContainer("mongo:7.0.8-jammy");
            m.start();

            TestPropertyValues.of("spring.data.mongodb.uri=" + m.getReplicaSetUrl()).applyTo(ctx);
        }
    }
}

package org.example.itest.db.mongodb.simple;

import org.bson.Document;
import org.junit.jupiter.api.Test;
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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = SimpleMongoAppITest.Init.class)
class SimpleMongoAppITest {
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    void test() {
        mongoTemplate.insert(new Document("name", "test"), "test");
        List<Document> list = mongoTemplate.findAll(Document.class, "test");
        assertThat(list, hasSize(1));
    }

    static class Init implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext ctx) {
            MongoDBContainer m = new MongoDBContainer("mongo:7.0.8-jammy");
            m.start();

            TestPropertyValues.of("spring.data.mongodb.uri=" + m.getReplicaSetUrl()).applyTo(ctx);
        }
    }
}

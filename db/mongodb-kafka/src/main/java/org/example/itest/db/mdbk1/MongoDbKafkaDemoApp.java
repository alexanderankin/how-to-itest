package org.example.itest.db.mdbk1;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@SpringBootApplication
class MongoDbKafkaDemoApp {
    public static void main(String[] args) {
        SpringApplication.run(MongoDbKafkaDemoApp.class, args);
    }

    @Configuration
    static class Config {
        @Bean
        NewTopic exampleTopic() {
            // DO NOT USE THIS IN PROD - instead add configuration for partition count
            // STOPSHIP
            return new NewTopic(Listeners.EXAMPLES_TOPIC, 1, (short) 1);
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    @Component
    static class Listeners {
        public static final String EXAMPLES_TOPIC = "examples";
        private final ExampleService exampleService;
        @KafkaListener(topics = EXAMPLES_TOPIC, groupId = "${spring.application.name:MongoDbKafkaDemoApp}")
        public void examples(@Payload Example example) {
            Example saved = exampleService.save(example);
            log.info("saved: {}", saved);
        }
    }

    @Data
    @Accessors(chain = true)
    @Document("example")
    static class Example {
        String id;
        String name;
    }

    @RequiredArgsConstructor
    @Service
    static class ExampleService {
        private final MongoTemplate mongoTemplate;

        Example save(Example example) {
            return mongoTemplate.save(example);
        }
    }
}

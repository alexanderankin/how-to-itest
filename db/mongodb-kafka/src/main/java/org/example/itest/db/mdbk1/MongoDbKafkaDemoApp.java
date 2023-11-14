package org.example.itest.db.mdbk1;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@SpringBootApplication
class MongoDbKafkaDemoApp {
    public static void main(String[] args) {
        SpringApplication.run(MongoDbKafkaDemoApp.class, args);
    }

    @Configuration
    static class Config {
    }

    @Slf4j
    @RequiredArgsConstructor
    @Component
    static class Listeners {
        private final ExampleService exampleService;
        @KafkaListener(topics = "examples")
        public void examples(Example example) {
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

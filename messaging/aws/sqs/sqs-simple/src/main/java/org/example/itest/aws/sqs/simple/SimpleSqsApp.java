package org.example.itest.aws.sqs.simple;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Optional;

@SpringBootApplication
class SimpleSqsApp {
    public static void main(String[] args) {
        SpringApplication.run(SimpleSqsApp.class, args);
    }

    @RequiredArgsConstructor
    @Slf4j
    @Component
    static class Comp {
        final SqsTemplate sqsTemplate;

        Optional<Message<String>> get() {
            return sqsTemplate.receive("test", String.class);
        }

        void send(String message) {
            var sent = sqsTemplate.send("test", message);
            log.info("{}", sent);
        }
    }
}

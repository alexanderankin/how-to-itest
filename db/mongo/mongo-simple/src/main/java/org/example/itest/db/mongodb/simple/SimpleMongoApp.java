package org.example.itest.db.mongodb.simple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
class SimpleMongoApp {
    public static void main(String[] args) {
        SpringApplication.run(SimpleMongoApp.class, args);
    }

    @RestController
    static class Controller {

    }
}

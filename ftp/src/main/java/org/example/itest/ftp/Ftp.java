package org.example.itest.ftp;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public class Ftp {
    @Accessors(chain = true)
    @Data
    @Component
    @ConfigurationProperties("example.ftp")
    public static class Properties {
        String host;
        int port;
        String folder;
    }

    @RequiredArgsConstructor
    @Configuration
    public static class Config {
        private final Properties properties;
        @SneakyThrows
        @Bean
        FTPClient ftpClient() {
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(properties.host);
            ftpClient.changeWorkingDirectory(properties.folder);
            return ftpClient;
        }
    }

    @RequiredArgsConstructor
    @org.springframework.stereotype.Service
    public static class Service {
        private final FTPClient client;

        @SneakyThrows
        String contents(String file) {
            return new String(client.retrieveFileStream(file).readAllBytes());
        }
    }

    @RequiredArgsConstructor
    @RestController
    public static class Controller {
        private final FTPClient client;

        @GetMapping("/files/{file}")
        StreamingResponseBody getFile(@PathVariable String file) {
            return outputStream -> client.retrieveFile(file, outputStream);
        }

    }
}

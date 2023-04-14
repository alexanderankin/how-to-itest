package org.example.itest.ftp;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

public class Ftp {

    @SneakyThrows
    public static void main(String[] args) {
        Properties properties = new Properties()
                .setHost("localhost")
                .setPort(21);

        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(new InetSocketAddress(properties.getHost(), properties.port).getAddress());
        ftpClient.login("toor", "root");
        ftpClient.changeWorkingDirectory(properties.folder);

        System.out.println(Arrays.asList(ftpClient.listDirectories()));
    }

    @Accessors(chain = true)
    @Data
    @Component
    @ConfigurationProperties("example.ftp")
    public static class Properties {
        String host;
        int port = 21;
        String folder;
        String username;
        @ToString.Exclude
        String password;
    }

    @Slf4j
    @RequiredArgsConstructor
    @Configuration
    public static class Config {
        private final Properties properties;

        @SneakyThrows
        @Bean
        FTPClient ftpClient() {
            FTPClient ftpClient = new FTPClient();
            ftpClient.addProtocolCommandListener(new LogCommandListener(log::debug));
            ftpClient.connect(InetAddress.getByName(properties.host), properties.port);
            Assert.isTrue(ftpClient.login(properties.username, properties.password), () -> "could not login with " + properties);
            Assert.isTrue(ftpClient.changeWorkingDirectory(properties.folder), () -> "could not go to dir " + properties.folder);
            return ftpClient;
        }

    }

    private static class LogCommandListener extends PrintCommandListener {
        public LogCommandListener(Consumer<String> logger) {
            super(new PrintWriter(new FtpCommandLoggingWriter(logger)));
        }

        private static class FtpCommandLoggingWriter extends Writer {
            private final StringBuilder stringBuilder;
            private final Consumer<String> logger;

            public FtpCommandLoggingWriter(Consumer<String> logger) {
                this.logger = logger;
                stringBuilder = new StringBuilder();
            }

            @Override
            public void write(@Nullable char[] cbuf, int off, int len) {
                if (cbuf != null) stringBuilder.append(cbuf, off, len);
            }

            @Override
            public void flush() {
                for (String s : stringBuilder.toString().split("\r\n")) {
                    logger.accept(s);
                }

                stringBuilder.setLength(0);
            }

            @Override
            public void close() {
            }
        }
    }

    @RequiredArgsConstructor
    @org.springframework.stereotype.Service
    public static class Service {
        private final FTPClient client;

        @SneakyThrows
        String contents(String file) {
            try (InputStream inputStream = client.retrieveFileStream(file)) {
                return new String(inputStream.readAllBytes());
            }
        }

        @SneakyThrows
        void write(String file, String contents) {
            InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
            client.enterLocalActiveMode();
            if (!client.storeFile(file, stream)) {
                throw new IllegalStateException("upload of " + file + " was not successful");
            }
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

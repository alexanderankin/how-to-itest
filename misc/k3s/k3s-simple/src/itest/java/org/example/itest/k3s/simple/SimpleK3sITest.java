package org.example.itest.k3s.simple;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleK3sITest {

    private static File k3sYaml;

    @SneakyThrows
    @BeforeAll
    static void setup() {
        @SuppressWarnings("resource")
        K3sContainer k3sContainer = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.30.1-k3s1"));
        k3sContainer.start();

        k3sYaml = File.createTempFile("k3sYaml", ".yaml");
        k3sYaml.deleteOnExit();
        Files.writeString(k3sYaml.toPath(), k3sContainer.getKubeConfigYaml(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("SameParameterValue")
    static String which(String executable) {
        for (String folder : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
            File folderFile = new File(folder);
            if (!folderFile.exists() || !folderFile.isDirectory()) continue;
            if (Arrays.asList(Objects.requireNonNull(folderFile.list(), "list is null")).contains(executable)) {
                return new File(folderFile, folder).getPath();
            }
        }
        return null;
    }

    @SneakyThrows
    @Test
    void test() {
        String kubectl = Optional.ofNullable(which("kubectl"))
                .orElse(
                        Path.of(System.getProperty("user.home"))
                                .resolve(Path.of(".asdf/shims/kubectl"))
                                .toString()
                );

        var pb = new ProcessBuilder(kubectl, "run", "busybox", "--image=busybox");
        pb.environment().put("KUBECONFIG", k3sYaml.getPath());
        pb.inheritIO();
        Process kubectlRun = pb.start();
        int exit = kubectlRun.waitFor();
        assertEquals(0, exit);
    }
}

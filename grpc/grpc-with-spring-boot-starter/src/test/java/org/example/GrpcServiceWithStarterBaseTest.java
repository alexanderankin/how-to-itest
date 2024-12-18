package org.example;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest // (webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        GrpcServiceWithStarterTestApp.TestcontainersConfig.class,
        ExampleServiceClientTestConfig.class,
})
@ActiveProfiles("itest")
public abstract class GrpcServiceWithStarterBaseTest {
}

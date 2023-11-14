package org.example.itest.pg.annotationtest;

import org.example.itest.pg.annotation.PostgresTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@PostgresTest
public class BaseITest {
}

package org.example.itest.pg.annotation;

import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(initializers = PostgresInitializer.class)
public @interface PostgresTest {
}

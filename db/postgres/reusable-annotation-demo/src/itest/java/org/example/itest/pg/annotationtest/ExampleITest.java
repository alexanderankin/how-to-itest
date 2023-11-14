package org.example.itest.pg.annotationtest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class ExampleITest extends BaseITest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void test() {
        jdbcTemplate.execute("create table abc(id serial, name varchar(500) unique)");
        jdbcTemplate.execute("insert into abc(name) values('example')");
        List<Map<String, Object>> examples = jdbcTemplate.query("select * from abc", new ColumnMapRowMapper());
        assertThat(examples, hasSize(1));
    }
}

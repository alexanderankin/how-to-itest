package org.example.itest.mysql.customurl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest
class MysqlCustomJdbcUrlAppITest {
    @Autowired
    JdbcClient jdbcClient;

    @Test
    void test() {
        jdbcClient.sql("create table if not exists example(id int not null auto_increment primary key, name varchar(255) unique);").update();

        jdbcClient.sql("insert into example(name) values('example1'), ('example2')").update();

        var examples = jdbcClient.sql("select * from example").query(Example.class).list();
        assertThat(examples, containsInAnyOrder(
                new Example(1, "example1"),
                new Example(2, "example2")
        ));
    }

    record Example(int id, String name) {
    }
}

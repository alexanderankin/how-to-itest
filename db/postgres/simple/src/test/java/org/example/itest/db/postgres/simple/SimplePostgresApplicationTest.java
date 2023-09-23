package org.example.itest.db.postgres.simple;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SimplePostgresApplicationTest {

    /**
     * @see <a href="https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/">vladmihalcea.com</a>
     */
    @Test
    void test() {
        SimplePostgresApplication.Example e1 = new SimplePostgresApplication.Example();
        SimplePostgresApplication.Example e2 = new SimplePostgresApplication.Example();

        assertThat(e1.equals(e2), is(false));
        assertThat(e1.hashCode() == e2.hashCode(), is(true));

        e1.setId(1);
        e2.setId(e1.getId());
        assertThat(e1.equals(e2), is(true));
    }

}

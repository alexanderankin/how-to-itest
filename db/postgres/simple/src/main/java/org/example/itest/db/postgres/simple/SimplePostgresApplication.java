package org.example.itest.db.postgres.simple;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.stereotype.Repository;

@EnableJpaRepositories(considerNestedRepositories = true)
@SpringBootApplication
class SimplePostgresApplication {
    public static void main(String[] args) {
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.datasource.url", "");
        System.setProperty("spring.datasource.username", "");
        System.setProperty("spring.datasource.password", "");
        SpringApplication.run(SimplePostgresApplication.class, args);
    }

    @Repository
    @RepositoryRestResource(path = "examples")
    interface ExampleRepository extends
            ListCrudRepository<Example, Integer>,
            PagingAndSortingRepository<Example, Integer> {
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @Entity(name = "example")
    static class Example extends RepresentationModel<Example> {
        @Id
        @GeneratedValue
        Integer id;
        @Column(nullable = false, unique = true)
        String name;
        String description;

        // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Example other)) return false;
            return id != null && id.equals(other.getId());
        }

        // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}

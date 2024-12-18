package org.example;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.example.proto.example.ExampleProtos;
import org.example.proto.example.ExampleServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
class GrpcServiceWithStarter {
    public static void main(String[] args) {
        SpringApplication.run(GrpcServiceWithStarter.class, args);
    }

    @Repository
    interface ExampleRepository extends JpaRepository<Example, UUID> {
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @ToString(onlyExplicitlyIncluded = true)
    @Data
    @Accessors(chain = true)
    @Entity
    @Table(name = "example")
    static class Example {
        @Id
        @GeneratedValue
        UUID id;

        @Size(min = 1, max = 150)
        @NotNull
        @Column(nullable = false)
        String name;

        String description;
    }

    @Component
    @Validated
    static class ExampleMapper {
        Example toEntity(ExampleProtos.Example request) {
            return new Example()
                    .setId(request.hasId() ? UUID.fromString(request.getId()) : null)
                    .setName(request.getName())
                    .setDescription(request.getDescription());
        }

        ExampleProtos.Example toProto(@Validated Example example) {
            return ExampleProtos.Example.newBuilder()
                    .setId(Optional.ofNullable(example.getId()).map(UUID::toString).orElse(null))
                    .setName(example.getName())
                    .setDescription(example.getDescription())
                    .build();
        }
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class ExampleService extends ExampleServiceGrpc.ExampleServiceImplBase {
        final ExampleRepository exampleRepository;
        final ExampleMapper exampleMapper;

        @Override
        public void createExample(ExampleProtos.Example request, StreamObserver<StringValue> responseObserver) {
            try {
                String id = exampleRepository.save(exampleMapper.toEntity(request).setId(null)).getId().toString();
                responseObserver.onNext(StringValue.newBuilder().setValue(id).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("error in createExample", e);
                responseObserver.onError(e);
            }
        }

        @Override
        public void getExamples(Empty request, StreamObserver<ExampleProtos.Example> responseObserver) {
            try {
                exampleRepository.findAll().stream().map(exampleMapper::toProto).forEach(responseObserver::onNext);
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("error in getExamples", e);
                responseObserver.onError(e);
            }
        }

        @SneakyThrows
        @Override
        public void getExample(StringValue request, StreamObserver<ExampleProtos.Example> responseObserver) {
            try {
                Example example = exampleRepository.findById(UUID.fromString(request.getValue())).orElseThrow(() -> new StatusException(Status.NOT_FOUND));
                responseObserver.onNext(exampleMapper.toProto(example));
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("error in getExample", e);
                responseObserver.onError(e);
            }
        }

        @Override
        public void updateExample(ExampleProtos.Example request, StreamObserver<ExampleProtos.Example> responseObserver) {
            try {
                Example example = exampleMapper.toEntity(request);
                Example save = exampleRepository.save(example);
                responseObserver.onNext(exampleMapper.toProto(save));
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("error in updateExample", e);
                responseObserver.onError(e);
            }
        }

        @Override
        public void deleteExample(StringValue request, StreamObserver<Empty> responseObserver) {
            try {
                exampleRepository.deleteById(UUID.fromString(request.getValue()));
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("error in deleteExample", e);
                responseObserver.onError(e);
            }
        }
    }
}

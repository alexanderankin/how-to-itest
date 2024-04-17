package org.example.itest.aws.sqs_sns.queue;

import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.policybuilder.iam.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;

@SpringBootApplication
class SqsSnsQueueApp {
    private final SnsTemplate snsTemplate;

    public SqsSnsQueueApp(SnsTemplate snsTemplate) {
        this.snsTemplate = snsTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(SqsSnsQueueApp.class, args);
    }

    @Data
    @Accessors(chain = true)
    @Component
    @ConfigurationProperties("queue-app")
    static class QueueAppProperties {
        String queue = "queue";
        String topic = "topic";
    }

    @RequiredArgsConstructor
    @Component
    @Slf4j
    @Getter
    static class AccountNumberProvider {
        final AwsCredentialsProvider awsCredentialsProvider;
        final AwsProperties awsProperties;
        private String account;
        private GetCallerIdentityResponse callerIdentity;

        @PostConstruct
        void initSts() {
            try (StsClient stsClient = StsClient.builder()
                    .credentialsProvider(awsCredentialsProvider)
                    .applyMutation(b -> b.endpointOverride(awsProperties.getEndpoint()))
                    .build()) {
                callerIdentity = stsClient.getCallerIdentity();
                account = callerIdentity.arn();
            }
            log.debug("found our account to be: {}", account);
        }
    }

    /**
     * awslocal sqs create-queue --queue-name test
     * awslocal sns create-topic --name test
     * awslocal sns list-topics # topic arn
     * awslocal sqs list-queues # get queueUrl
     * awslocal sqs get-queue-attributes --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/test/ --attribute-names QueueArn # get queue arn
     * awslocal sns subscribe --topic-arn arn:aws:sns:us-east-1:000000000000:test --protocol sqs --notification-endpoint arn:aws:sqs:us-east-1:000000000000:test
     */
    @AllArgsConstructor
    @Component
    @Slf4j
    static class TopicQueueInitializer {
        SqsAsyncClient sqsClient;
        SnsClient snsClient;
        SqsTemplate sqsTemplate;
        SnsTemplate snsTemplate;
        QueueAppProperties queueAppProperties;
        AccountNumberProvider accountNumberProvider;

        @SneakyThrows
        @PostConstruct
        void init() {
            log.debug("TopicQueueInitializer.init running");
            CreateTopicResponse topic = snsClient.createTopic(CreateTopicRequest.builder()
                    .name(queueAppProperties.getTopic())
                    .build());

            log.debug("got a topic: {}", topic);

            try {
                GetQueueUrlResponse urlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                                .queueName(queueAppProperties.getQueue())
                                .build())
                        .join();

                log.debug("got an existing queue ({}), deleting: {}", urlResponse.queueUrl(), urlResponse);

                sqsClient.deleteQueue(DeleteQueueRequest.builder()
                        .queueUrl(urlResponse.queueUrl())
                        .build());

                log.debug("deleted successfully");
            } catch (CompletionException completionException) {
                log.trace("caught a completionException but that doesn't mean anything");
                Throwable cause = completionException.getCause();
                try {
                    throw cause != null ? cause : new RuntimeException(completionException);
                } catch (QueueDoesNotExistException ignored) {
                    log.debug("QueueDoesNotExistException, meaning, nothing to delete");
                } catch (Throwable e) {
                    log.debug("Misc throwable from getting/deleting the existing q:", e);
                    throw new RuntimeException(e);
                }
            }

            CreateQueueResponse queueResponse = null;
            try {
                queueResponse = sqsClient.createQueue(CreateQueueRequest.builder()
                                .queueName(queueAppProperties.getQueue())
                                .build())
                        .join();

                log.debug("created a queue: {}", queueResponse);
            } catch (CompletionException ce) {
                try {
                    throw ce.getCause() != null ? ce.getCause() : ce;
                } catch (QueueDeletedRecentlyException e) {
                    log.debug("QueueDeletedRecentlyException, waiting:");
                    Thread.sleep(Duration.ofMinutes(1).plusSeconds(5));

                    queueResponse = sqsClient.createQueue(CreateQueueRequest.builder()
                                    .queueName(queueAppProperties.getQueue())
                                    .build())
                            .join();

                    log.debug("created a queue after 1min delay: {}", queueResponse);
                } catch (Exception e) {
                    var r = new RuntimeException(e);
                    log.debug("unexpected exception from createQueue", r);
                    throw r;
                }
            }

            String queueArn;
            try {
                queueArn = sqsClient.getQueueAttributes(
                                GetQueueAttributesRequest.builder()
                                        .queueUrl(queueResponse.queueUrl())
                                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                                        .build())
                        .join()
                        .attributes().get(QueueAttributeName.QUEUE_ARN);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            // Set the policy on the SQS queue
            SetQueueAttributesRequest setQueueAttributesRequest = SetQueueAttributesRequest.builder()
                    .queueUrl(queueResponse.queueUrl())
                    .attributes(Map.of(QueueAttributeName.POLICY,
                            IamPolicy.builder()
                                    .addStatement(IamStatement.builder()
                                            .effect(IamEffect.ALLOW)
                                            .addPrincipal(IamPrincipal.ALL)
                                            .addAction(IamAction.create("sqs:SendMessage"))
                                            // .addResource(IamResource.create("arn:aws:sqs:us-east-1:000000000000:" + queueAppProperties.getQueue()))
                                            .addResource(IamResource.create(queueArn))
                                            .addCondition(IamCondition.builder()
                                                    .operator(IamConditionOperator.ARN_EQUALS)
                                                    .key("aws:SourceArn")
                                                    .value(topic.topicArn())
                                                    .build())
                                            .build())
                                    .build()
                                    .toJson()))
                    .build();

            try {
                sqsClient.setQueueAttributes(setQueueAttributesRequest).join();

                log.debug("Permission granted for SNS topic to send messages to SQS queue.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            snsClient.subscribe(SubscribeRequest.builder()
                    .topicArn(topic.topicArn())
                    .protocol("sqs")
                    .endpoint(queueArn)
                    .build());

            // snsTemplate.convertAndSend(queueAppProperties.getTopic(), "stuff");
            //
            // try {
            //     Optional<Message<String>> receive = sqsTemplate.receive(queueAppProperties.getQueue(), String.class);
            //     System.out.println(receive);
            // } catch (Throwable e) {
            //     log.error("error", e);
            // }
            // System.out.println();

            // snsClient.getTopicAttributes(GetTopicAttributesRequest.builder()
            //         .topicArn()
            //         .build());
            //
            // // snsClient.getTopicAttributes()
            // //
            // // snsClient.addPermission(AddPermissionRequest.builder()
            // //         .topicArn()
            // //         .build());
        }
    }

    @RestController
    static class QueueController {
        @Autowired
        SqsTemplate sqsTemplate;
        @Autowired
        SnsTemplate snsTemplate;
        @Autowired
        QueueAppProperties properties;

        @GetMapping("/messages")
        List<String> messages() {
            Collection<Message<String>> messages = sqsTemplate.receiveMany(properties.getQueue(), String.class);
            return messages.stream().map(Message::getPayload).toList();
        }

        @PostMapping("/messages")
        @ResponseStatus(HttpStatus.CREATED)
        void sendMessage(@RequestBody String message) {
            snsTemplate.convertAndSend(properties.getTopic(), message);
        }
    }
}

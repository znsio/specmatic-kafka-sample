package com.sample.springbootkafkademo;

import in.specmatic.kafka.mock.KafkaMock;
import in.specmatic.kafka.mock.api.Expectation;
import in.specmatic.kafka.mock.api.VerificationResult;
import in.specmatic.test.SpecmaticJUnitSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = {SpringbootKafkaDemoApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ContractTest extends SpecmaticJUnitSupport {
    private static KafkaMock kafkaMock = null;
    private static ConfigurableApplicationContext context = null;

    @BeforeAll
    public static void setUp() {
        System.setProperty("host", "localhost");
        System.setProperty("port", "8080");
        System.setProperty("CUSTOM_RESPONSE", "true");

        List<String> fileList= new ArrayList<>();
        fileList.add("src/test/resources/kafka_stub.yaml");

        // kafkaMock is a static variable
        kafkaMock = KafkaMock.fromAsyncAPIFiles(
                        fileList,
                        9092,
                        "./kafka-logs");

        kafkaMock.start();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        Thread.sleep(5000);
        if (kafkaMock != null) {
            int count = waitForKafkaMessages();
            Assertions.assertNotEquals(0, count);
            try {
                Expectation expectation=new Expectation("firstTopic",1);
                List <Expectation>expectationList=new ArrayList<>();
                expectationList.add(expectation);
                kafkaMock.setExpectations(expectationList);
                VerificationResult verificationResult = kafkaMock.verifyExpectations();
                assert  verificationResult.getSuccess();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            kafkaMock.stop();
        }
        if (context != null) context.close();

    }

    private static int waitForKafkaMessages() throws InterruptedException {
        int millisecondsWaited = 0;
        int count = 0;
        int sleepInterval = 1000;

        while ((kafkaMock.getMessageHistory().size() == 0 && millisecondsWaited < 5000) || kafkaMock.getMessageHistory().size() != count) {
            count = kafkaMock.getMessageHistory().size();
            System.out.println("Count is "+count+" and message history is "+kafkaMock.getMessageHistory());
            Thread.sleep(sleepInterval);
            millisecondsWaited += sleepInterval;
        }
        System.out.println("Total Kafka messages received by Specmatic: " + count);
        return count;
    }
}
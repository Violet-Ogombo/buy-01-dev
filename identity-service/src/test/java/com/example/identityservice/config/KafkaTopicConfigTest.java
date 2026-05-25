package com.example.identityservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicConfigTest {

    private KafkaTopicConfig kafkaTopicConfig = new KafkaTopicConfig();

    @Test
    void userRegisteredTopic_createsNewTopicWithCorrectName() {
        NewTopic topic = kafkaTopicConfig.userRegisteredTopic();
        assertThat(topic).isNotNull();
        assertThat(topic.name()).isEqualTo("user-registered");
    }

    @Test
    void userRegisteredTopic_setsPartitionsToOne() {
        NewTopic topic = kafkaTopicConfig.userRegisteredTopic();
        assertThat(topic.numPartitions()).isEqualTo(1);
    }

    @Test
    void userRegisteredTopic_setsReplicasToOne() {
        NewTopic topic = kafkaTopicConfig.userRegisteredTopic();
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void userRegisteredTopic_createsNewInstanceEachTime() {
        NewTopic topic1 = kafkaTopicConfig.userRegisteredTopic();
        NewTopic topic2 = kafkaTopicConfig.userRegisteredTopic();
        
        // Each call creates a new instance
        assertThat(topic1).isNotSameAs(topic2);
        // But they have the same configuration
        assertThat(topic1.name()).isEqualTo(topic2.name());
        assertThat(topic1.numPartitions()).isEqualTo(topic2.numPartitions());
    }
}

// OrderServiceKafkaConfig.java
package org.kosa.orderservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.kosa.orderservice.dto.UserWithdrawalEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class OrderServiceKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     *  사용자 탈퇴 이벤트 Consumer Factory (MSA 환경 대응)
     */
    @Bean
    public ConsumerFactory<String, UserWithdrawalEvent> userWithdrawalConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // 기본 설정
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-group");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        //  에러 핸들링 역직렬화 설정
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);

        // 실제 역직렬화 클래스 설정
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                StringDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class);

        //  MSA 환경을 위한 JSON 역직렬화 설정
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "org.kosa.orderservice.dto.UserWithdrawalEvent");

        //  User Service → Order Service 클래스 매핑
        configProps.put(JsonDeserializer.TYPE_MAPPINGS,
                "org.kosa.userservice.dto.UserWithdrawalEvent:org.kosa.orderservice.dto.UserWithdrawalEvent");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Listener Container Factory (에러 처리 강화)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserWithdrawalEvent>
    userWithdrawalKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, UserWithdrawalEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(userWithdrawalConsumerFactory());

        //  에러 핸들러 설정 (재시도 로직)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3L)  // 1초 간격으로 3번 재시도
        );

        // 역직렬화 에러는 재시도하지 않음 (데이터 문제일 가능성 높음)
        errorHandler.addNotRetryableExceptions(
                org.springframework.messaging.converter.MessageConversionException.class,
                org.apache.kafka.common.errors.RecordDeserializationException.class
        );

        factory.setCommonErrorHandler(errorHandler);

        // 수동 커밋 설정 (안전한 메시지 처리)
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    /**
     *  String 메시지용 Consumer Factory (디버깅/fallback용)
     */
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-string-group");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    stringKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory());

        return factory;
    }
}
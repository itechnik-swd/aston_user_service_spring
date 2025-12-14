package ru.astondevs.kafka;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.astondevs.kafka.event.UserEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;

    public void sendUserEvent(UserEvent userEvent) {
        try {
            kafkaTemplate.send(userEventsTopic, userEvent.email(), userEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("User event sent successfully: {} to topic: {}",
                                    userEvent.eventType(), userEventsTopic);
                        } else {
                            log.error("Failed to send user event: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending user event to Kafka", e);
        }
    }
}

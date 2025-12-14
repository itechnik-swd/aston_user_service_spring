package ru.astondevs.kafka.event;

import ru.astondevs.enums.EventType;

import java.time.LocalDateTime;

public record UserEvent(
        Long userId,
        String email,
        String username,
        EventType eventType,
        LocalDateTime timestamp
) {
}

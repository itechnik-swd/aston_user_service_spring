package ru.astondevs.kafka.event;

import ru.astondevs.enums.EventType;

public record UserEvent(
        EventType eventType,
        String email
) {
}

package ru.astondevs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateUserRequestDTO (
        String name,

        @Email(message = "Email should be valid")
        String email,

        @PositiveOrZero(message = "Age must be positive or zero")
        Integer age
) {
}
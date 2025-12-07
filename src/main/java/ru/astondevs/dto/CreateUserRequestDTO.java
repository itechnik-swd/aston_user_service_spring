package ru.astondevs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateUserRequestDTO(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 25, message = "Name must be less than 25 characters")
        String name,

        @Email(message = "Email should be valid")
        @NotBlank(message = "Email must not be blank")
        @Size(max = 50, message = "Email must be less than 50 characters")
        String email,

        @PositiveOrZero(message = "Age must be positive or zero")
        @NotNull(message = "Age must not be null")
        int age
) {
}

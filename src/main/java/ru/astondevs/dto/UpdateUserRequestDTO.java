package ru.astondevs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class UpdateUserRequestDTO {

    private String name;

    @Email(message = "Email should be valid")
    private String email;

    @PositiveOrZero(message = "Age must be positive or zero")
    private int age;
}
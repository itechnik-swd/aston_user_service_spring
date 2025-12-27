package ru.astondevs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.astondevs.dto.CreateUserRequestDTO;
import ru.astondevs.dto.UpdateUserRequestDTO;
import ru.astondevs.dto.UserResponseDTO;
import ru.astondevs.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<EntityModel<UserResponseDTO>> createUser(
            @Valid @RequestBody CreateUserRequestDTO createUserRequestDTO) {

        UserResponseDTO createdUser = userService.createUser(createUserRequestDTO);
        EntityModel<UserResponseDTO> model = EntityModel.of(createdUser);
        Link selfLink = WebMvcLinkBuilder.linkTo(UserController.class)
                .slash(createdUser.userId())
                .withSelfRel();
        model.add(selfLink);
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponseDTO>> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        EntityModel<UserResponseDTO> model = EntityModel.of(user);
        Link selfLink = WebMvcLinkBuilder.linkTo(UserController.class)
                .slash(id)
                .withSelfRel();
        model.add(selfLink);
        return ResponseEntity.ok(model);
    }

    @GetMapping
    public ResponseEntity<List<EntityModel<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        List<EntityModel<UserResponseDTO>> models = users.stream()
                .map(this::toEntityModel)
                .toList();
        return ResponseEntity.ok(models);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponseDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDTO updateUserRequestDTO) {

        UserResponseDTO updatedUser = userService.updateUser(id, updateUserRequestDTO);
        EntityModel<UserResponseDTO> model = EntityModel.of(updatedUser);
        Link selfLink = WebMvcLinkBuilder.linkTo(UserController.class)
                .slash(id)
                .withSelfRel();
        model.add(selfLink);
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UserResponseDTO> toEntityModel(UserResponseDTO user) {
        EntityModel<UserResponseDTO> model = EntityModel.of(user);
        Link link = WebMvcLinkBuilder.linkTo(UserController.class)
                .slash(user.userId())
                .withSelfRel();
        model.add(link);
        return model;
    }
}
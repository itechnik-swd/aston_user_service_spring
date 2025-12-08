package ru.astondevs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.astondevs.dto.CreateUserRequestDTO;
import ru.astondevs.dto.UpdateUserRequestDTO;
import ru.astondevs.entity.User;
import ru.astondevs.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerMockMvcTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
            System.out.println("PostgresSQL container stopped");
        }
    }

    @Test
    void createUser_ShouldCreateAndReturnUser_WhenValidRequest() throws Exception {
        // Given
        CreateUserRequestDTO createUserRequestDTO = new CreateUserRequestDTO(
                "John Doe", "john@example.com", 30);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.name").value(createUserRequestDTO.name()))
                .andExpect(jsonPath("$.email").value(createUserRequestDTO.email()))
                .andExpect(jsonPath("$.age").value(createUserRequestDTO.age()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // Verify in database
        assertThat(userRepository.count()).isEqualTo(1);
        Iterable<User> users = userRepository.findAll();
        User savedUser = users.iterator().next();
        assertThat(savedUser.getName()).isEqualTo(createUserRequestDTO.name());
        assertThat(savedUser.getEmail()).isEqualTo(createUserRequestDTO.email());
        assertThat(savedUser.getAge()).isEqualTo(createUserRequestDTO.age());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenMissingRequiredFields() throws Exception {
        // Given
        CreateUserRequestDTO request = new CreateUserRequestDTO(
                null, null, -1);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Name must not be blank"))
                .andExpect(jsonPath("$.email").value("Email must not be blank"))
                .andExpect(jsonPath("$.age").value("Age must be positive or zero"));
    }

    @Test
    void createUser_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        // Given
        User existingUser = new User();
        existingUser.setName("Existing User");
        existingUser.setEmail("existing@example.com");
        existingUser.setAge(25);
        userRepository.save(existingUser);

        CreateUserRequestDTO createUserRequestDTO = new CreateUserRequestDTO(
                "New User", "existing@example.com", 30);

        String errorMessage = String.format("User with email %s already exists",
                createUserRequestDTO.email());

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value(errorMessage));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() throws Exception {
        // Given
        User user1 = new User();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");
        user1.setAge(20);

        User user2 = new User();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        user2.setAge(25);

        userRepository.saveAll(java.util.List.of(user1, user2));

        // When & Then
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value(user1.getName()))
                .andExpect(jsonPath("$[1].name").value(user2.getName()));
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExists() throws Exception {
        // Given
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setAge(30);
        User savedUser = userRepository.save(user);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value(user.getName()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.age").value(user.getAge()));
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Given
        long nonExistentId = 999L;
        String errorMessage = String.format("User with id %d not found", nonExistentId);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value(errorMessage));
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenValidRequest() throws Exception {
        // Given
        User user = new User();
        user.setName("Original Name");
        user.setEmail("original@example.com");
        user.setAge(25);
        User savedUser = userRepository.save(user);

        UpdateUserRequestDTO updateUserRequestDTO = new UpdateUserRequestDTO(
                "Updated Name", "updated@example.com", 30);

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updateUserRequestDTO.name()))
                .andExpect(jsonPath("$.email").value(updateUserRequestDTO.email()))
                .andExpect(jsonPath("$.age").value(updateUserRequestDTO.age()));

        // Verify update in database
        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo(updateUserRequestDTO.name());
        assertThat(updatedUser.getEmail()).isEqualTo(updateUserRequestDTO.email());
        assertThat(updatedUser.getAge()).isEqualTo(updateUserRequestDTO.age());
    }

    @Test
    void updateUser_ShouldHandlePartialUpdate() throws Exception {
        // Given
        User user = new User();
        user.setName("Original Name");
        user.setEmail("original@example.com");
        user.setAge(25);
        User savedUser = userRepository.save(user);

        UpdateUserRequestDTO updateUserRequestDTO = new UpdateUserRequestDTO(
                "Updated Name", null, null);

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updateUserRequestDTO.name()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.age").value(user.getAge()));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenExists() throws Exception {
        // Given
        User user = new User();
        user.setName("User to Delete");
        user.setEmail("delete@example.com");
        user.setAge(40);
        User savedUser = userRepository.save(user);

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", savedUser.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(userRepository.existsById(savedUser.getId())).isFalse();
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Given
        long nonExistentId = 999L;

        String errorMessage = String.format("User with id %d not found", nonExistentId);

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }
}

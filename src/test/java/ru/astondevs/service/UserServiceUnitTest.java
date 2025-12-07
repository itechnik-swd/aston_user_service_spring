package ru.astondevs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.astondevs.dto.CreateUserRequestDTO;
import ru.astondevs.dto.UpdateUserRequestDTO;
import ru.astondevs.dto.UserResponseDTO;
import ru.astondevs.entity.User;
import ru.astondevs.exception.UserAlreadyExistsException;
import ru.astondevs.exception.UserNotFoundException;
import ru.astondevs.mapper.UserMapper;
import ru.astondevs.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_ShouldCreateUser_WhenEmailIsUnique() {
        // Given
        CreateUserRequestDTO createUserRequestDTO = new CreateUserRequestDTO(
                "John Doe",
                "john@example.com",
                30);

        User user = userMapper.toUser(createUserRequestDTO);
        User savedUser = new User(1L, "John Doe", "john@example.com", 30,
                LocalDateTime.now(), LocalDateTime.now());
        UserResponseDTO expectedDTO = userMapper.toUserResponseDTO(savedUser);

        // When
        when(userRepository.existsByEmail(createUserRequestDTO.email())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(savedUser);
        UserResponseDTO result = userService.createUser(createUserRequestDTO);

        // Then
        assertThat(result).isEqualTo(expectedDTO);
        verify(userRepository).existsByEmail(createUserRequestDTO.email());
        verify(userRepository).save(user);
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailAlreadyExists() {
        // Given
        CreateUserRequestDTO createUserRequestDTO = new CreateUserRequestDTO(
                "John Doe",
                "existing@example.com",
                30);

        // When
        when(userRepository.existsByEmail(createUserRequestDTO.email())).thenReturn(true);

        // Then
        assertThatThrownBy(() -> userService.createUser(createUserRequestDTO))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User with email existing@example.com already exists");
        verify(userRepository).existsByEmail(createUserRequestDTO.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Given
        User user = new User(1L, "John Doe", "john@example.com", 30,
                LocalDateTime.now(), LocalDateTime.now());
        long userId = user.getId();
        UserResponseDTO expectedDTO = userMapper.toUserResponseDTO(user);

        // When
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UserResponseDTO result = userService.getUserById(userId);

        // Then
        assertThat(result).isEqualTo(expectedDTO);
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotExists() {
        // Given
        long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id %d not found", userId);
        verify(userRepository).findById(userId);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        User user1 = new User(1L, "User1", "user1@example.com", 20,
                LocalDateTime.now(), LocalDateTime.now());
        User user2 = new User(2L, "User2", "user2@example.com", 25,
                LocalDateTime.now(), LocalDateTime.now());

        // When
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        List<UserResponseDTO> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenValidData() {
        // Given
        Long userId = 1L;
        UpdateUserRequestDTO updateUserRequestDTO = new UpdateUserRequestDTO(
                "New Name",
                "new@example.com",
                35);

        User existingUser = new User(userId, "Old Name", "old@example.com",
                30, LocalDateTime.now(), LocalDateTime.now());
        User updatedUser = new User(userId, "New Name", "new@example.com", 35
                , LocalDateTime.now(), LocalDateTime.now());
        UserResponseDTO expectedDTO = userMapper.toUserResponseDTO(updatedUser);

        // When
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot(updateUserRequestDTO.email(), userId)).thenReturn(false);
        when(userRepository.save(existingUser)).thenReturn(updatedUser);

        UserResponseDTO result = userService.updateUser(userId, updateUserRequestDTO);

        // Then
        assertThat(result).isEqualTo(expectedDTO);
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmailAndIdNot("new@example.com", userId);
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserNotExists() {
        // Given
        long userId = 999L;
        UpdateUserRequestDTO updateUserRequestDTO = new UpdateUserRequestDTO(
                "New Name",
                "new@example.com",
                35);

        // When
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequestDTO))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id %d not found", userId);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        // Given
        long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserNotExists() {
        // Given
        long userId = 999L;

        // When & Then
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id %d not found", userId);

        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(anyLong());
    }
}

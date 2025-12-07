package ru.astondevs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astondevs.dto.CreateUserRequestDTO;
import ru.astondevs.dto.UpdateUserRequestDTO;
import ru.astondevs.dto.UserResponseDTO;
import ru.astondevs.entity.User;
import ru.astondevs.exception.UserAlreadyExistsException;
import ru.astondevs.exception.UserNotFoundException;
import ru.astondevs.mapper.UserMapper;
import ru.astondevs.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDTO createUser(CreateUserRequestDTO createUserRequestDTO) {
        if (userRepository.existsByEmail(createUserRequestDTO.email())) {
            throw new UserAlreadyExistsException(
                    String.format("User with email %s already exists",
                            createUserRequestDTO.email())
            );
        }

        User user = userMapper.toUser(createUserRequestDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toUserResponseDTO(savedUser);
    }

    public UserResponseDTO getUserById(Long id) {
        User user = findUserById(id);
        return userMapper.toUserResponseDTO(user);
    }

    public List<UserResponseDTO> getAllUsers() {
        List<UserResponseDTO> users = new ArrayList<>();

        for (User user : userRepository.findAll()) {
            users.add(userMapper.toUserResponseDTO(user));
        }

        return users;
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO updateUserRequestDTO) {
        User user = findUserById(id);

        if (updateUserRequestDTO.email() != null &&
                !updateUserRequestDTO.email().equals(user.getEmail()) &&
                userRepository.existsByEmailAndIdNot(updateUserRequestDTO.email(), id)) {
                throw new UserAlreadyExistsException(
                        String.format("User with email %s already exists",
                                updateUserRequestDTO.email())
                );
            }


        userMapper.updateUser(updateUserRequestDTO, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toUserResponseDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(
                    String.format("User with id %d not found", id)
            );
        }
        userRepository.deleteById(id);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        String.format("User with id %d not found", id)
                ));
    }
}

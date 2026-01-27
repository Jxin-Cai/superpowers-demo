package com.example.cms.application.service;

import com.example.cms.domain.model.user.*;
import com.example.cms.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String password, String email) {
        Username uname = Username.of(username);
        if (userRepository.existsByUsername(uname)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        User user = User.builder()
                .username(uname)
                .password(Password.fromEncoded(passwordEncoder.encode(password)))
                .email(email != null ? Email.of(email) : null)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(Username.of(username));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(UserId.of(id));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User createAdmin(String username, String password, String email) {
        Username uname = Username.of(username);
        if (userRepository.existsByUsername(uname)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        User user = User.builder()
                .username(uname)
                .password(Password.fromEncoded(passwordEncoder.encode(password)))
                .email(email != null ? Email.of(email) : null)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User updateRole(Long id, Role newRole) {
        User user = userRepository.findById(UserId.of(id))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.changeRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(UserId.of(id));
    }

    public boolean verifyPassword(String username, String rawPassword) {
        return findByUsername(username)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword().getEncodedValue()))
                .orElse(false);
    }
}

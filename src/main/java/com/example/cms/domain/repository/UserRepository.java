package com.example.cms.domain.repository;

import com.example.cms.domain.model.user.User;
import com.example.cms.domain.model.user.UserId;
import com.example.cms.domain.model.user.Username;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(Username username);
    List<User> findAll();
    void deleteById(UserId id);
    boolean existsByUsername(Username username);
}

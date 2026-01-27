package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.user.User;
import com.example.cms.domain.model.user.UserId;
import com.example.cms.domain.model.user.Username;
import com.example.cms.domain.repository.UserRepository;
import com.example.cms.infrastructure.persistence.entity.UserEntity;
import com.example.cms.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity saved = springDataUserRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataUserRepository.findById(id.getValue())
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return springDataUserRepository.findByUsername(username.getValue())
                .map(userMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UserId id) {
        springDataUserRepository.deleteById(id.getValue());
    }

    @Override
    public boolean existsByUsername(Username username) {
        return springDataUserRepository.existsByUsername(username.getValue());
    }
}

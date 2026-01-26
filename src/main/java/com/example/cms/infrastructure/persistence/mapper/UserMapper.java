package com.example.cms.infrastructure.persistence.mapper;

import com.example.cms.domain.model.user.*;
import com.example.cms.domain.shared.Audit;
import com.example.cms.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return User.builder()
                .id(UserId.of(entity.getId()))
                .username(Username.of(entity.getUsername()))
                .password(Password.fromEncoded(entity.getPassword()))
                .email(entity.getEmail() != null ? Email.of(entity.getEmail()) : null)
                .role(Role.from(entity.getRole()))
                .status(UserStatus.from(entity.getStatus()))
                .audit(Audit.of(entity.getCreatedAt(), entity.getUpdatedAt()))
                .build();
    }

    public UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId().getValue());
        }
        entity.setUsername(domain.getUsername().getValue());
        entity.setPassword(domain.getPassword().getEncodedValue());
        domain.getEmailValue().ifPresent(entity::setEmail);
        entity.setRole(domain.getRole().getValue());
        entity.setStatus(domain.getStatus().getValue());
        return entity;
    }
}

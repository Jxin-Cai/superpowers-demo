package com.example.cms.domain.model.user;

import com.example.cms.domain.shared.Audit;
import lombok.Getter;

import java.util.Optional;

@Getter
public class User {
    private final UserId id;
    private final Username username;
    private final Password password;
    private final Email email;
    private Role role;
    private UserStatus status;
    private Audit audit;

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.password = builder.password;
        this.email = builder.email;
        this.role = builder.role != null ? builder.role : Role.USER;
        this.status = builder.status != null ? builder.status : UserStatus.ACTIVE;
        this.audit = builder.audit != null ? builder.audit : Audit.create();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getEmailValue() {
        return Optional.ofNullable(email).map(Email::getValue);
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
        this.audit = this.audit.markModified();
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
        this.audit = this.audit.markModified();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.audit = this.audit.markModified();
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public static class Builder {
        private UserId id;
        private Username username;
        private Password password;
        private Email email;
        private Role role;
        private UserStatus status;
        private Audit audit;

        public Builder id(UserId id) {
            this.id = id;
            return this;
        }

        public Builder username(Username username) {
            this.username = username;
            return this;
        }

        public Builder password(Password password) {
            this.password = password;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder audit(Audit audit) {
            this.audit = audit;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}

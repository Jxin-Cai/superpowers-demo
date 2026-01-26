package com.example.cms.domain.model.category;

import com.example.cms.domain.shared.Audit;
import lombok.Getter;

import java.util.Optional;

@Getter
public class Category {
    private final Long id;
    private String name;
    private String description;
    private Audit audit;

    private Category(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.audit = builder.audit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void rename(String name) {
        this.name = name;
        this.audit = this.audit.markModified();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.audit = this.audit.markModified();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private Audit audit = Audit.create();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder audit(Audit audit) {
            this.audit = audit;
            return this;
        }

        public Category build() {
            return new Category(this);
        }
    }
}

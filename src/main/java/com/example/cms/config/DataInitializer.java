package com.example.cms.config;

import com.example.cms.application.service.UserService;
import com.example.cms.domain.repository.UserRepository;
import com.example.cms.domain.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(String... args) {
        if (userRepository.findAll().stream().noneMatch(User::isAdmin)) {
            userService.createAdmin("admin", "admin123", null);
            System.out.println("默认管理员账号已创建: admin / admin123");
        }
    }
}

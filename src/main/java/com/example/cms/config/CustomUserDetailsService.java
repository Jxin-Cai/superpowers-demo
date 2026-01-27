package com.example.cms.config;

import com.example.cms.domain.model.user.User;
import com.example.cms.domain.model.user.Username;
import com.example.cms.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security UserDetailsService实现
 * 用于加载用户信息进行认证
 *
 * @author ${git config user.name}
 * @date 2026-01-27
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(Username.of(username))
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername().getValue())
                .password(user.getPassword().getEncodedValue())
                .authorities(Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }
}

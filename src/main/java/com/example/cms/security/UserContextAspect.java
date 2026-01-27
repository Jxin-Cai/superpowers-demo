package com.example.cms.security;

import com.example.cms.domain.model.user.User;
import com.example.cms.domain.model.user.Username;
import com.example.cms.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class UserContextAspect {

    private final UserRepository userRepository;

    /**
     * 拦截所有Controller方法，在执行前设置用户上下文
     * 使用execution切点匹配controller包下的所有方法
     */
    @Around("execution(* com.example.cms.presentation.controller..*.*(..))")
    public Object setUserContext(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                userRepository.findByUsername(Username.of(username))
                        .map(User::getId)
                        .ifPresent(UserContext::setCurrent);
            }
            return pjp.proceed();
        } finally {
            UserContext.clear();
        }
    }
}

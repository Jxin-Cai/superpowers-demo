package com.example.cms.config;

import com.example.cms.security.CurrentUserMethodArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserMethodArgumentResolver);
    }

    /**
     * 配置静态资源处理，支持Vue Router的HTML5 History模式
     * 所有非API请求都回退到index.html，由前端路由处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource requestedResource = location.createRelative(resourcePath);
                    // 如果资源存在，直接返回
                    if (requestedResource.exists() && requestedResource.isReadable()) {
                        return requestedResource;
                    }
                    // 如果是API请求或H2控制台，不做回退（返回null让Spring继续处理）
                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("h2-console/")) {
                        return null;
                    }
                    // 其他所有请求（包括前端路由如/login、/register、/admin等）都回退到index.html
                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}

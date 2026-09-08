package com.ecommerce.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/uploads/}")
    private String baseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("Configuring resource handler for /uploads/** -> file:{}, baseUrl={}", uploadDir, baseUrl);

        String normalizedDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        Path uploadPath = Paths.get(normalizedDir).toAbsolutePath().normalize();

        String resourceLocation = "file:" + uploadPath.toString().replace("\\", "/") + "/";

        log.info("Registering resource handler: /uploads/** -> {}", resourceLocation);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.debug("Configuring CORS for /uploads/**");
        registry.addMapping("/uploads/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:3001")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}

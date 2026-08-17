package com.GSU26SE22_SU26SE002.RealMateAI.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CORSConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5173",
                        "http://localhost:3000",
                        "http://103.161.180.17:8081",
                        "http://103.161.180.17",
                        "https://sep490-gsu26se22.vercel.app",
                        "https://vercel.com/harrrys-projects-252f0bb1/sep490-gsu26se22/JSaWbrZACbTXPBvBX7oF3NjEMEsM"
                )
                .allowedHeaders("*")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Methods", "Access-Control-Allow-Headers")
                .allowedMethods("*")
                .maxAge(1440000);

        // Thêm cấu hình cho Swagger
        registry.addMapping("/v2/api-docs/**").allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        registry.addMapping("/swagger-resources/**").allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        registry.addMapping("/swagger-ui.html").allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        registry.addMapping("/webjars/**").allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        registry.addMapping("/swagger-ui/**").allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
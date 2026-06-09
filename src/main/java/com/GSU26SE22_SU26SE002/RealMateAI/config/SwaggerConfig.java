package com.GSU26SE22_SU26SE002.RealMateAI.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("API Documentation")
                        .description("API for your project")
                        .version("1.0"))
                .servers(Arrays.asList(

                        new Server().url("http://103.161.180.17:8081").description("Production Server URL"),

                        new Server().url("http://localhost:8080").description("Local Server URL")

                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearer-key", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                )
                .addSecurityItem(new SecurityRequirement().addList("bearer-key"));
    }
}

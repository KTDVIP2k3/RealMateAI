package com.GSU26SE22_SU26SE002.RealMateAI.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Bean
    public OpenApiCustomizer customerGlobalOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getTags() != null) {
                List<Tag> cleanTags = openApi.getTags().stream().map(tag -> {
                    tag.setName(formatTagName(tag.getName()));
                    return tag;
                }).collect(Collectors.toList());
                openApi.setTags(cleanTags);
            }

            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem ->
                        pathItem.readOperations().forEach(operation -> {
                            if (operation.getTags() != null) {
                                List<String> newTags = operation.getTags().stream()
                                        .map(this::formatTagName)
                                        .collect(Collectors.toList());
                                operation.setTags(newTags);
                            }
                        })
                );
            }
        };
    }

    private String formatTagName(String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            return tagName;
        }

        String cleanName = tagName.endsWith("-controller") ? tagName.replace("-controller", "") : tagName;

        StringBuilder result = new StringBuilder();
        String[] words = cleanName.split("-");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1));
            }
        }

        return result.toString();
    }
}
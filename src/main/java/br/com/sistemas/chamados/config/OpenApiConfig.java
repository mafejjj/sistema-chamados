package br.com.sistemas.chamados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Documentação da API disponível em /swagger-ui.html e /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI chamadosOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Sistema de Chamados API")
            .version("1.0.0")
            .description("API REST em arquitetura em camadas para clientes, categorias, "
                + "técnicos, chamados, comentários e dashboard."));
    }
}

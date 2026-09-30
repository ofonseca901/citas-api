package co.fcv.citas.adapter.security;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
  @Bean OpenAPI citasOpenApi() {
    return new OpenAPI().info(new Info().title("FCV Citas API").version("S5").description("Laboratorio con datos sintéticos; las operaciones conservan autorización por rol y ownership."))
      .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
  }
}

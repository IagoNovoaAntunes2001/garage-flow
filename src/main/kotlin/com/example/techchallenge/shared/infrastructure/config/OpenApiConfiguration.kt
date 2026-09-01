package com.example.techchallenge.shared.infrastructure.config

import com.example.techchallenge.shared.api.error.ApiError
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    private val apiErrorSchema = ModelConverters.getInstance().read(ApiError::class.java)

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(Info().title("Garage Flow API").version("1.0.0").description("Automotive repair shop Phase 1 MVP backend"))
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            ).schemas(apiErrorSchema),
        )
}

package com.example.techchallenge.shared.api

import com.example.techchallenge.shared.api.error.GlobalExceptionHandler
import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.infrastructure.observability.CorrelationIdFilter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(FoundationTestController::class)
@Import(
    GlobalExceptionHandler::class,
    CorrelationIdFilter::class,
    PermitAllSecurity::class,
)
class GlobalExceptionHandlerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `returns field violations in the standard error body`() {
        mockMvc.post("/test/validation") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"name":""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.error") { value("Bad Request") }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.path") { value("/test/validation") }
            jsonPath("$.correlationId") { value(not(nullValue())) }
            jsonPath("$.violations[0].field") { value("name") }
            jsonPath("$.stackTrace") { doesNotExist() }
        }
    }

    @Test
    fun `maps domain exceptions without exposing implementation details`() {
        mockMvc.get("/test/domain/missing") {
            header(CorrelationIdFilter.HEADER_NAME, "request-123")
        }.andExpect {
            status { isNotFound() }
            header { string(CorrelationIdFilter.HEADER_NAME, "request-123") }
            jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
            jsonPath("$.message") { value("Test resource not found") }
            jsonPath("$.correlationId") { value("request-123") }
            jsonPath("$.stackTrace") { doesNotExist() }
        }

        mockMvc.get("/test/domain/conflict").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("CONFLICT") }
        }
    }

    @Test
    fun `rejects invalid page bounds consistently`() {
        mockMvc.get("/test/page") {
            param("page", "-1")
            param("size", "101")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PAGINATION") }
            jsonPath("$.stackTrace") { doesNotExist() }
        }
    }

}

data class FoundationTestRequest(
    @field:NotBlank val name: String,
)

@RestController
class FoundationTestController {
    @PostMapping("/test/validation")
    fun validate(@Valid @RequestBody request: FoundationTestRequest) = HttpStatus.OK

    @GetMapping("/test/domain/{kind}")
    fun domainError(@PathVariable kind: String): Nothing = when (kind) {
        "missing" -> throw ResourceNotFoundException("Test resource not found")
        else -> throw ConflictException("Test conflict")
    }

    @GetMapping("/test/page")
    fun page(
        @RequestParam page: Int,
        @RequestParam size: Int,
    ) = PageRequestDto.of(page, size)
}

@TestConfiguration
class PermitAllSecurity {
    @Bean
    fun testSecurity(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .authorizeHttpRequests { it.anyRequest().permitAll() }
        .httpBasic(withDefaults())
        .build()
}

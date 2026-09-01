package com.example.techchallenge.catalog.api.controller

import com.example.techchallenge.catalog.domain.model.CatalogService
import com.example.techchallenge.catalog.domain.repository.CatalogServicePage
import com.example.techchallenge.catalog.application.usecase.CatalogServiceUseCases
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.api.PageResponse
import com.example.techchallenge.shared.domain.CatalogServiceId
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class CatalogServiceRequest(
    @field:NotBlank @field:Size(min = 2, max = 120) val name: String,
    @field:NotBlank @field:Size(min = 2, max = 1000) val description: String,
    @field:DecimalMin("0.00") val price: BigDecimal,
    val active: Boolean = true,
)

data class CatalogServiceResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val price: String,
    val currency: String,
    val active: Boolean,
)

@RestController
@RequestMapping("/api/v1/admin/services")
@SecurityRequirement(name = "bearerAuth")
class CatalogServiceController(private val useCases: CatalogServiceUseCases) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CatalogServiceRequest): CatalogServiceResponse =
        useCases.create(request.name, request.description, request.price).toResponse()

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): CatalogServiceResponse = useCases.get(CatalogServiceId(id)).toResponse()

    @GetMapping
    fun list(@RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int): PageResponse<CatalogServiceResponse> =
        useCases.list(PageRequestDto.of(page, size)).toResponse()

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: CatalogServiceRequest): CatalogServiceResponse =
        useCases.update(CatalogServiceId(id), request.name, request.description, request.price, request.active).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable id: UUID) = useCases.remove(CatalogServiceId(id))
}

private fun CatalogService.toResponse() = CatalogServiceResponse(id.value, name, description, currentPrice.amount.toPlainString(), currentPrice.currency, active)

private fun CatalogServicePage.toResponse() = PageResponse(content.map { it.toResponse() }, page, size, totalElements, totalPages)

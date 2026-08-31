package com.example.techchallenge.inventory.api.controller

import com.example.techchallenge.inventory.application.usecase.InventoryUseCases
import com.example.techchallenge.inventory.application.usecase.StockOperation
import com.example.techchallenge.inventory.domain.model.InventoryItem
import com.example.techchallenge.inventory.domain.model.InventoryItemType
import com.example.techchallenge.inventory.domain.repository.InventoryItemPage
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.api.PageResponse
import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.InventoryItemId
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
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

data class InventoryItemRequest(
    val type: InventoryItemType,
    @field:NotBlank @field:Size(min = 2, max = 120) val name: String,
    @field:NotBlank @field:Size(min = 2, max = 1000) val description: String,
    @field:DecimalMin("0.00") val unitPrice: BigDecimal,
    @field:Min(0) val availableQuantity: Long = 0,
    val active: Boolean = true,
)

data class InventoryStockAdjustmentRequest(
    val operation: StockOperation,
    @field:Min(1) val quantity: Long,
    @field:NotBlank @field:Size(max = 500) val reason: String,
)

data class InventoryItemResponse(
    val id: UUID,
    val type: InventoryItemType,
    val name: String,
    val description: String,
    val unitPrice: String,
    val currency: String,
    val availableQuantity: Long,
    val active: Boolean,
)

@RestController
@RequestMapping("/api/v1/admin/inventory-items")
class InventoryController(private val useCases: InventoryUseCases) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: InventoryItemRequest): InventoryItemResponse =
        useCases.create(request.type, request.name, request.description, request.unitPrice, request.availableQuantity).toResponse()

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): InventoryItemResponse = useCases.get(InventoryItemId(id)).toResponse()

    @GetMapping
    fun list(
        @RequestParam(required = false) type: InventoryItemType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<InventoryItemResponse> = useCases.list(type, PageRequestDto.of(page, size)).toResponse()

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: InventoryItemRequest): InventoryItemResponse =
        useCases.update(InventoryItemId(id), request.name, request.description, request.unitPrice, request.active).toResponse()

    @PostMapping("/{id}/stock-adjustments", "/{id}/adjustments")
    fun adjustStock(
        @PathVariable id: UUID,
        @Valid @RequestBody request: InventoryStockAdjustmentRequest,
        authentication: Authentication,
    ): InventoryItemResponse = useCases.adjustStock(
        InventoryItemId(id),
        request.quantity,
        request.operation,
        request.reason,
        AdministratorId(UUID.fromString((authentication.principal as Jwt).getClaimAsString("administratorId"))),
    ).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable id: UUID) = useCases.remove(InventoryItemId(id))
}

private fun InventoryItem.toResponse() = InventoryItemResponse(
    id.value, type, name, description, unitPrice.amount.toPlainString(), unitPrice.currency, availableQuantity, active,
)

private fun InventoryItemPage.toResponse() = PageResponse(content.map { it.toResponse() }, page, size, totalElements, totalPages)

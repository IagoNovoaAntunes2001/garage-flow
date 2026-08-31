package com.example.techchallenge.customer.api.controller

import com.example.techchallenge.customer.api.CustomerCreateRequest
import com.example.techchallenge.customer.api.CustomerPageResponse
import com.example.techchallenge.customer.api.CustomerResponse
import com.example.techchallenge.customer.api.CustomerUpdateRequest
import com.example.techchallenge.customer.api.toResponse
import com.example.techchallenge.customer.application.usecase.CustomerUseCases
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.domain.CustomerId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/customers")
class CustomerController(private val useCases: CustomerUseCases) {
    @PostMapping
    fun create(@Valid @RequestBody request: CustomerCreateRequest): ResponseEntity<CustomerResponse> {
        val response = useCases.create(request.document, request.name, request.email, request.phone).toResponse()
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id).toUri()
        return ResponseEntity.created(location).body(response)
    }

    @GetMapping("/{customerId}")
    fun get(@PathVariable customerId: UUID) = useCases.get(CustomerId(customerId)).toResponse()

    @GetMapping
    fun list(
        @RequestParam(required = false) document: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): CustomerPageResponse = useCases.list(document, PageRequestDto.of(page, size)).toResponse()

    @PutMapping("/{customerId}")
    fun update(@PathVariable customerId: UUID, @Valid @RequestBody request: CustomerUpdateRequest) =
        useCases.update(CustomerId(customerId), request.name, request.email, request.phone, request.active).toResponse()

    @DeleteMapping("/{customerId}")
    fun remove(@PathVariable customerId: UUID): ResponseEntity<Void> {
        useCases.remove(CustomerId(customerId))
        return ResponseEntity.noContent().build()
    }
}

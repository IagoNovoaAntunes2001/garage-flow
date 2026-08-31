package com.example.techchallenge.serviceorder.api.controller

import com.example.techchallenge.serviceorder.api.ServiceOrderDetailResponse
import com.example.techchallenge.serviceorder.api.toCustomerTrackingResponse
import com.example.techchallenge.serviceorder.api.toDetailResponse
import com.example.techchallenge.serviceorder.application.usecase.QueryServiceOrders
import com.example.techchallenge.serviceorder.application.usecase.TrackServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.api.PageRequestDto
import com.example.techchallenge.shared.api.PageResponse
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ServiceOrderQueryController(
    private val query: QueryServiceOrders,
    private val tracking: TrackServiceOrder,
) {
    @GetMapping("/api/v1/admin/service-orders/{serviceOrderId}")
    fun detail(@PathVariable serviceOrderId: UUID): ServiceOrderDetailResponse =
        query.get(ServiceOrderId(serviceOrderId)).toDetailResponse()

    @GetMapping("/api/v1/admin/service-orders")
    fun list(
        @RequestParam(required = false) status: ServiceOrderStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<ServiceOrderDetailResponse> {
        val result = query.list(status, PageRequestDto.of(page, size))
        return PageResponse(result.content.map { it.toDetailResponse() }, result.page, result.size, result.totalElements, result.totalPages)
    }

    @GetMapping("/api/v1/tracking/service-orders/{serviceOrderId}")
    fun track(
        @PathVariable serviceOrderId: UUID,
        @RequestHeader("X-Service-Order-Token") token: String,
    ) = tracking.execute(ServiceOrderId(serviceOrderId), token).toCustomerTrackingResponse()
}

package com.example.techchallenge.serviceorder.api.controller

import com.example.techchallenge.serviceorder.api.ApprovalRequest
import com.example.techchallenge.serviceorder.api.CreateServiceOrderRequest
import com.example.techchallenge.serviceorder.api.CreateServiceOrderResponse
import com.example.techchallenge.serviceorder.api.OrderActionResponse
import com.example.techchallenge.serviceorder.api.RepairItemsRequest
import com.example.techchallenge.serviceorder.api.toDetailResponse
import com.example.techchallenge.serviceorder.api.toCustomerTrackingResponse
import com.example.techchallenge.serviceorder.application.usecase.AddAdditionalRepairs
import com.example.techchallenge.serviceorder.application.usecase.CompleteServiceOrder
import com.example.techchallenge.serviceorder.application.usecase.CreateServiceOrder
import com.example.techchallenge.serviceorder.application.usecase.DecideQuotation
import com.example.techchallenge.serviceorder.application.usecase.PrepareQuotation
import com.example.techchallenge.serviceorder.application.usecase.StartExecution
import com.example.techchallenge.shared.domain.AdministratorId
import com.example.techchallenge.shared.domain.ServiceOrderId
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ServiceOrderCommandController(
    private val createServiceOrder: CreateServiceOrder,
    private val prepareQuotation: PrepareQuotation,
    private val decideQuotation: DecideQuotation,
    private val startExecution: StartExecution,
    private val addAdditionalRepairs: AddAdditionalRepairs,
    private val completeServiceOrder: CompleteServiceOrder,
) {
    @PostMapping("/api/v1/admin/service-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    fun create(@Valid @RequestBody request: CreateServiceOrderRequest): CreateServiceOrderResponse {
        val result = createServiceOrder.execute(request.customerDocument, com.example.techchallenge.shared.domain.VehicleId(request.vehicleId), request.items.map { it.toCommand() })
        return CreateServiceOrderResponse(result.order.toDetailResponse(), result.trackingToken)
    }

    @PostMapping("/api/v1/admin/service-orders/{serviceOrderId}/diagnosis/start")
    @SecurityRequirement(name = "bearerAuth")
    fun startDiagnosis(@PathVariable serviceOrderId: UUID, authentication: Authentication): OrderActionResponse =
        OrderActionResponse(prepareQuotation.startDiagnosis(ServiceOrderId(serviceOrderId), actorId(authentication)).toDetailResponse())

    @PostMapping("/api/v1/admin/service-orders/{serviceOrderId}/quotation/request-approval")
    @SecurityRequirement(name = "bearerAuth")
    fun requestApproval(
        @PathVariable serviceOrderId: UUID,
        @Valid @RequestBody(required = false) request: RepairItemsRequest?,
        authentication: Authentication,
    ): OrderActionResponse = OrderActionResponse(
        prepareQuotation.requestApproval(ServiceOrderId(serviceOrderId), request?.items.orEmpty().map { it.toCommand() }, actorId(authentication)).toDetailResponse(),
    )

    @PostMapping("/api/v1/admin/service-orders/{serviceOrderId}/additional-repairs")
    @SecurityRequirement(name = "bearerAuth")
    fun additionalRepairs(
        @PathVariable serviceOrderId: UUID,
        @Valid @RequestBody request: RepairItemsRequest,
        authentication: Authentication,
    ): OrderActionResponse = OrderActionResponse(
        addAdditionalRepairs.execute(ServiceOrderId(serviceOrderId), request.items.map { it.toCommand() }, actorId(authentication)).toDetailResponse(),
    )

    @PostMapping("/api/v1/admin/service-orders/{serviceOrderId}/execution/start")
    @SecurityRequirement(name = "bearerAuth")
    fun startExecution(@PathVariable serviceOrderId: UUID, authentication: Authentication): OrderActionResponse =
        OrderActionResponse(startExecution.execute(ServiceOrderId(serviceOrderId), AdministratorId(UUID.fromString(actorId(authentication)))).toDetailResponse())

    @PostMapping("/api/v1/admin/service-orders/{serviceOrderId}/finish")
    @SecurityRequirement(name = "bearerAuth")
    fun finish(@PathVariable serviceOrderId: UUID, authentication: Authentication): OrderActionResponse =
        OrderActionResponse(completeServiceOrder.finish(ServiceOrderId(serviceOrderId), actorId(authentication)).toDetailResponse())

    @PostMapping("/api/v1/admin/service-orders/{serviceOrderId}/delivery")
    @SecurityRequirement(name = "bearerAuth")
    fun deliver(@PathVariable serviceOrderId: UUID, authentication: Authentication): OrderActionResponse =
        OrderActionResponse(completeServiceOrder.deliver(ServiceOrderId(serviceOrderId), actorId(authentication)).toDetailResponse())

    @PostMapping("/api/v1/customer-approvals/{serviceOrderId}/approve")
    fun approve(
        @PathVariable serviceOrderId: UUID,
        @RequestHeader("X-Service-Order-Token") token: String,
        @Valid @RequestBody request: ApprovalRequest,
    ) = decideQuotation.approve(ServiceOrderId(serviceOrderId), token, request.quotationVersion).toCustomerTrackingResponse()

    @PostMapping("/api/v1/customer-approvals/{serviceOrderId}/reject")
    fun reject(
        @PathVariable serviceOrderId: UUID,
        @RequestHeader("X-Service-Order-Token") token: String,
        @Valid @RequestBody request: ApprovalRequest,
    ) = decideQuotation.reject(ServiceOrderId(serviceOrderId), token, request.quotationVersion, request.reason).toCustomerTrackingResponse()

    private fun actorId(authentication: Authentication): String = (authentication.principal as Jwt).getClaimAsString("administratorId")
}

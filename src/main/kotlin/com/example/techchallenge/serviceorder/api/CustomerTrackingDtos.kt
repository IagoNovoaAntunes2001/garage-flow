package com.example.techchallenge.serviceorder.api

import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import java.time.Instant
import java.util.UUID

data class CustomerTrackingResponse(
    val serviceOrderId: UUID,
    val status: ServiceOrderStatus,
    val progress: String,
    val currentQuotation: QuotationResponse?,
    val lastUpdatedAt: Instant,
)

fun ServiceOrder.toCustomerTrackingResponse() = CustomerTrackingResponse(
    id.value,
    status,
    when (status) {
        ServiceOrderStatus.RECEIVED -> "Service order received"
        ServiceOrderStatus.IN_DIAGNOSIS -> "Vehicle diagnosis is in progress"
        ServiceOrderStatus.AWAITING_APPROVAL -> "Quotation is awaiting customer approval"
        ServiceOrderStatus.IN_EXECUTION -> "Approved service is in execution"
        ServiceOrderStatus.FINISHED -> "Service is finished"
        ServiceOrderStatus.DELIVERED -> "Vehicle delivered"
    },
    currentQuotation?.toResponse(),
    updatedAt,
)

package com.example.techchallenge.serviceorder.domain

import com.example.techchallenge.serviceorder.domain.model.CustomerSnapshot
import com.example.techchallenge.serviceorder.domain.model.ItemSourceType
import com.example.techchallenge.serviceorder.domain.model.ServiceOrder
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderItem
import com.example.techchallenge.serviceorder.domain.model.VehicleSnapshot
import com.example.techchallenge.shared.domain.CustomerId
import com.example.techchallenge.shared.domain.DocumentType
import com.example.techchallenge.shared.domain.Money
import com.example.techchallenge.shared.domain.ServiceOrderId
import com.example.techchallenge.shared.domain.VehicleId
import java.time.Instant
import java.util.UUID

internal fun serviceOrder(now: Instant = Instant.parse("2026-08-31T12:00:00Z")) = ServiceOrder.create(
    ServiceOrderId.new(),
    CustomerSnapshot(CustomerId.new(), DocumentType.CPF, "***.982.247-**", "Maria Silva"),
    VehicleSnapshot(VehicleId.new(), "ABC1D23", "Ford", "Ka", 2020),
    listOf(
        ServiceOrderItem(UUID.randomUUID(), ItemSourceType.SERVICE, UUID.randomUUID(), "Oil Change", 1, Money.of("120.00")),
        ServiceOrderItem(UUID.randomUUID(), ItemSourceType.PART, UUID.randomUUID(), "Oil Filter", 2, Money.of("35.00")),
    ),
    "a".repeat(64),
    now.plusSeconds(3600),
    now,
)

package com.example.techchallenge.serviceorder.domain

import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.domain.DomainValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class ServiceOrderLifecycleTest {
    @Test
    fun `follows valid lifecycle transitions`() {
        val base = Instant.parse("2026-08-31T12:00:00Z")
        val waiting = serviceOrder(base).startDiagnosis(base.plusSeconds(1), "admin").requestApproval(emptyList(), base.plusSeconds(2), "admin")
        val approved = waiting.approveQuotation(1, base.plusSeconds(3))
        val executing = approved.startExecution(approved.items.map { it.id }.toSet(), base.plusSeconds(4), "admin")
        val finished = executing.finish(base.plusSeconds(5), "admin")
        val delivered = finished.deliver(base.plusSeconds(6), "admin")

        assertEquals(ServiceOrderStatus.DELIVERED, delivered.status)
        assertEquals(listOf(ServiceOrderStatus.RECEIVED, ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.AWAITING_APPROVAL, ServiceOrderStatus.IN_EXECUTION, ServiceOrderStatus.FINISHED, ServiceOrderStatus.DELIVERED), delivered.statusHistory.map { it.toStatus })
    }

    @Test
    fun `rejects invalid lifecycle transitions`() {
        assertThrows(DomainValidationException::class.java) { serviceOrder().finish(Instant.now(), "admin") }
        assertThrows(DomainValidationException::class.java) { serviceOrder().startExecution(emptySet(), Instant.now(), "admin") }
    }
}

package com.example.techchallenge.metrics.application

import com.example.techchallenge.serviceorder.application.port.ExecutionHistoryEvent
import com.example.techchallenge.serviceorder.application.port.OrderExecutionHistory
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class ExecutionTimeCalculatorTest {
    @Test
    fun `calculates average active execution seconds`() {
        val base = Instant.parse("2026-08-31T12:00:00Z")
        val histories = listOf(
            OrderExecutionHistory(ServiceOrderId.new(), listOf(ExecutionHistoryEvent(ServiceOrderStatus.IN_EXECUTION, base), ExecutionHistoryEvent(ServiceOrderStatus.FINISHED, base.plusSeconds(60)))),
            OrderExecutionHistory(ServiceOrderId.new(), listOf(ExecutionHistoryEvent(ServiceOrderStatus.IN_EXECUTION, base), ExecutionHistoryEvent(ServiceOrderStatus.FINISHED, base.plusSeconds(120)))),
        )

        assertEquals(90.0, ExecutionTimeCalculator().averageExecutionSeconds(histories))
    }

    @Test
    fun `returns null for empty completed execution set`() {
        assertNull(ExecutionTimeCalculator().averageExecutionSeconds(emptyList()))
    }
}

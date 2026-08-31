package com.example.techchallenge.metrics.application

import com.example.techchallenge.serviceorder.application.port.OrderExecutionHistory
import com.example.techchallenge.serviceorder.domain.model.ServiceOrderStatus
import java.time.Duration

class ExecutionTimeCalculator {
    fun averageExecutionSeconds(histories: List<OrderExecutionHistory>): Double? {
        val durations = histories.mapNotNull { history ->
            val start = history.events.firstOrNull { it.status == ServiceOrderStatus.IN_EXECUTION }?.occurredAt
            val finish = history.events.firstOrNull { it.status == ServiceOrderStatus.FINISHED && start != null && !it.occurredAt.isBefore(start) }?.occurredAt
            if (start == null || finish == null) null else Duration.between(start, finish).seconds
        }
        if (durations.isEmpty()) return null
        return durations.average()
    }
}

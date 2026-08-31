package com.example.techchallenge.metrics.application

import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class ExecutionTimeSummary(
    val from: Instant,
    val to: Instant,
    val orderCount: Int,
    val averageExecutionSeconds: Double?,
)

@Service
class GetAverageExecutionTime(
    private val orders: ServiceOrderRepository,
) {
    private val calculator = ExecutionTimeCalculator()

    @Transactional(readOnly = true)
    fun execute(from: Instant, to: Instant): ExecutionTimeSummary {
        if (!from.isBefore(to)) {
            throw DomainValidationException(ErrorCode.INVALID_PAGINATION, "Metric start must be before end")
        }
        val histories = orders.executionHistory(from, to)
        return ExecutionTimeSummary(from, to, histories.size, calculator.averageExecutionSeconds(histories))
    }
}

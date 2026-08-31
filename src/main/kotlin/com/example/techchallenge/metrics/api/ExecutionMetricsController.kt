package com.example.techchallenge.metrics.api

import com.example.techchallenge.metrics.application.ExecutionTimeSummary
import com.example.techchallenge.metrics.application.GetAverageExecutionTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ExecutionMetricsController(private val getAverageExecutionTime: GetAverageExecutionTime) {
    @GetMapping("/api/v1/admin/metrics/execution-time")
    fun average(@RequestParam from: Instant, @RequestParam to: Instant): ExecutionTimeSummary =
        getAverageExecutionTime.execute(from, to)
}

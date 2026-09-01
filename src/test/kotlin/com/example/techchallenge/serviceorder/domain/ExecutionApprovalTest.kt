package com.example.techchallenge.serviceorder.domain

import com.example.techchallenge.shared.domain.DomainValidationException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class ExecutionApprovalTest {
    @Test
    fun `does not enter execution while quotation approval is pending or rejected`() {
        val waiting = serviceOrder().startDiagnosis(Instant.now(), "admin").requestApproval(emptyList(), Instant.now(), "admin")

        assertThrows(DomainValidationException::class.java) { waiting.startExecution(emptySet(), Instant.now(), "admin") }

        val rejected = waiting.rejectQuotation(1, "Too expensive", Instant.now())
        assertThrows(DomainValidationException::class.java) { rejected.startExecution(emptySet(), Instant.now(), "admin") }
    }
}

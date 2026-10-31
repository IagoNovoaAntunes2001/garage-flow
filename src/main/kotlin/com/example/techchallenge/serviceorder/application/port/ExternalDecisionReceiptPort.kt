package com.example.techchallenge.serviceorder.application.port

import com.example.techchallenge.serviceorder.domain.model.ApprovalDecision
import java.time.Instant
import java.util.UUID

interface ExternalDecisionReceiptPort {
    fun insert(eventId: String, hash: String, orderId: UUID, version: Int, decision: ApprovalDecision, now: Instant): Boolean
    fun hashFor(eventId: String): String?
}

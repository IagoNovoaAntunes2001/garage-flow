package com.example.techchallenge.serviceorder.infrastructure.persistence

import com.example.techchallenge.serviceorder.application.port.ExternalDecisionReceiptPort
import com.example.techchallenge.serviceorder.domain.model.ApprovalDecision
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class ExternalDecisionReceiptAdapter(private val jdbc: JdbcTemplate) : ExternalDecisionReceiptPort {
    override fun insert(eventId: String, hash: String, orderId: UUID, version: Int, decision: ApprovalDecision, now: Instant): Boolean =
        jdbc.update(
            """INSERT INTO external_decision_receipts (event_id, payload_hash, service_order_id, quotation_version, decision, created_at)
               VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING""",
            eventId, hash, orderId, version, decision.name, Timestamp.from(now),
        ) == 1

    override fun hashFor(eventId: String): String? =
        jdbc.query("SELECT payload_hash FROM external_decision_receipts WHERE event_id = ?", { result, _ -> result.getString(1) }, eventId).firstOrNull()
}

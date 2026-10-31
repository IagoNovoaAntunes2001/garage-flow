package com.example.techchallenge.serviceorder.application.usecase

import com.example.techchallenge.serviceorder.application.port.ExternalDecisionReceiptPort
import com.example.techchallenge.serviceorder.application.port.ServiceOrderRepository
import com.example.techchallenge.serviceorder.application.ServiceOrderAuditLogger
import com.example.techchallenge.serviceorder.domain.model.ApprovalChannel
import com.example.techchallenge.serviceorder.domain.model.ApprovalDecision
import com.example.techchallenge.shared.domain.ClockProvider
import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.domain.ServiceOrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.UUID

@Service
class DecideExternalQuotation(
    private val orders: ServiceOrderRepository,
    private val receipts: ExternalDecisionReceiptPort,
    private val clock: ClockProvider,
    private val audit: ServiceOrderAuditLogger,
) {
    @Transactional
    fun execute(eventId: String, orderId: UUID, version: Int, decision: ApprovalDecision, reason: String?) {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(listOf(orderId.toString(), version.toString(), decision.name, if (reason == null) "0" else "1$reason").joinToString("\u0000").toByteArray())
            .joinToString("") { "%02x".format(it) }
        val order = orders.findByIdForUpdate(ServiceOrderId(orderId)) ?: throw ResourceNotFoundException("ServiceOrder not found")
        val now = clock.now()
        if (!receipts.insert(eventId, hash, orderId, version, decision, now)) {
            if (receipts.hashFor(eventId) != hash) throw ConflictException("Event already used for a different decision")
            return
        }
        val updated = when (decision) {
            ApprovalDecision.APPROVED -> order.approveQuotation(version, now, ApprovalChannel.EXTERNAL_INTEGRATION)
            ApprovalDecision.REJECTED -> order.rejectQuotation(version, reason, now, ApprovalChannel.EXTERNAL_INTEGRATION)
        }
        orders.save(updated)
        audit.approval(ServiceOrderId(orderId), decision == ApprovalDecision.APPROVED)
    }
}

package com.example.techchallenge.serviceorder.domain.model

import com.example.techchallenge.shared.domain.ApprovalId
import com.example.techchallenge.shared.domain.QuotationId
import java.time.Instant

enum class ApprovalDecision { APPROVED, REJECTED }
enum class ApprovalChannel { CUSTOMER_ACCESS_TOKEN, EXTERNAL_INTEGRATION }

data class Approval(
    val id: ApprovalId,
    val quotationId: QuotationId,
    val decision: ApprovalDecision,
    val decidedAt: Instant,
    val channel: ApprovalChannel,
    val reason: String?,
) {
    companion object {
        fun approve(quotationId: QuotationId, now: Instant, channel: ApprovalChannel = ApprovalChannel.CUSTOMER_ACCESS_TOKEN): Approval =
            Approval(ApprovalId.new(), quotationId, ApprovalDecision.APPROVED, now, channel, null)

        fun reject(quotationId: QuotationId, reason: String?, now: Instant, channel: ApprovalChannel = ApprovalChannel.CUSTOMER_ACCESS_TOKEN): Approval =
            Approval(ApprovalId.new(), quotationId, ApprovalDecision.REJECTED, now, channel, reason?.trim()?.takeIf { it.isNotBlank() })
    }
}

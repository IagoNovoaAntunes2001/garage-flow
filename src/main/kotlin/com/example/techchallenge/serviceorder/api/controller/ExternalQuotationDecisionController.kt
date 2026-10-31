package com.example.techchallenge.serviceorder.api.controller

import com.example.techchallenge.serviceorder.application.usecase.DecideExternalQuotation
import com.example.techchallenge.serviceorder.domain.model.ApprovalDecision
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.util.UUID

data class ExternalQuotationDecisionRequest(
    @field:NotBlank @field:Size(max = 150) val eventId: String,
    val serviceOrderId: UUID,
    @field:Min(1) val quotationVersion: Int,
    val decision: ApprovalDecision,
    @field:Size(max = 500) val reason: String? = null,
)

@RestController
class ExternalQuotationDecisionController(
    private val decide: DecideExternalQuotation,
    @param:Value("\${garage-flow.integration.quotation-secret:}") private val secret: String,
) {
    @PostMapping("/api/v1/integrations/quotation-decisions")
    @SecurityRequirement(name = "integrationToken")
    fun decide(
        @Parameter(hidden = true) @RequestHeader(value = "X-Integration-Token", required = false) token: String?,
        @Valid @RequestBody request: ExternalQuotationDecisionRequest,
    ): Map<String, Any> {
        if (secret.length < 32 || token == null || !MessageDigest.isEqual(secret.toByteArray(), token.toByteArray())) {
            throw BadCredentialsException("Invalid integration credentials")
        }
        decide.execute(request.eventId, request.serviceOrderId, request.quotationVersion, request.decision, request.reason)
        return mapOf("serviceOrderId" to request.serviceOrderId, "quotationVersion" to request.quotationVersion, "decision" to request.decision.name)
    }
}

package com.example.techchallenge.serviceorder.domain.model

import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ErrorCode

object ServiceOrderLifecycle {
    fun ensure(current: ServiceOrderStatus, expected: ServiceOrderStatus, action: String) {
        if (current != expected) invalid("Cannot $action when ServiceOrder is $current")
    }

    fun invalid(message: String): Nothing =
        throw DomainValidationException(ErrorCode.INVALID_SERVICE_ORDER_TRANSITION, message)

    fun approvalRequired(): Nothing =
        throw DomainValidationException(ErrorCode.QUOTATION_APPROVAL_REQUIRED, "Current quotation must be approved before execution")

    fun staleApproval(): Nothing =
        throw ConflictException("Approval does not match the current quotation version")
}

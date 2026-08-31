package com.example.techchallenge.shared.api.error

import com.example.techchallenge.shared.domain.BusinessRuleException
import com.example.techchallenge.shared.domain.ConflictException
import com.example.techchallenge.shared.domain.DomainException
import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ResourceNotFoundException
import com.example.techchallenge.shared.infrastructure.observability.CorrelationIdFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val violations = exception.bindingResult.fieldErrors
            .map { FieldViolation(it.field, it.defaultMessage ?: "Invalid value") }
            .sortedBy(FieldViolation::field)
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, violations)
    }

    @ExceptionHandler(
        ConstraintViolationException::class,
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class,
    )
    fun handleMalformedInput(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> = response(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        safeMessage(exception, "Invalid request"),
        request,
    )

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        exception: DomainException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val status = when (exception) {
            is DomainValidationException -> HttpStatus.BAD_REQUEST
            is ResourceNotFoundException -> HttpStatus.NOT_FOUND
            is ConflictException -> HttpStatus.CONFLICT
            is BusinessRuleException -> HttpStatus.UNPROCESSABLE_ENTITY
        }
        return response(status, exception.code.name, exception.message.orEmpty(), request)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataConflict(
        exception: DataIntegrityViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> = response(
        HttpStatus.CONFLICT,
        "CONFLICT",
        "The request conflicts with existing data",
        request,
    )

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(
        exception: AuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> = response(
        HttpStatus.UNAUTHORIZED,
        "UNAUTHORIZED",
        "Authentication is required",
        request,
    )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        exception: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> = response(
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "Access is denied",
        request,
    )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        logger.error("Unexpected request failure correlationId={}", correlationId(request), exception)
        return response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            request,
        )
    }

    private fun response(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        violations: List<FieldViolation> = emptyList(),
    ): ResponseEntity<ApiError> = ResponseEntity.status(status).body(
        ApiError(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase,
            code = code,
            message = message,
            path = request.requestURI,
            correlationId = correlationId(request),
            violations = violations,
        ),
    )

    private fun correlationId(request: HttpServletRequest): String =
        request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME)?.toString()
            ?: request.getHeader(CorrelationIdFilter.HEADER_NAME)
            ?: "unavailable"

    private fun safeMessage(exception: Exception, fallback: String): String =
        exception.message?.substringBefore(';')?.take(200) ?: fallback

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}

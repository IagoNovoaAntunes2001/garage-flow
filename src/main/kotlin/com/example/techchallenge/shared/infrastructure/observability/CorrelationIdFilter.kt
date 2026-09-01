package com.example.techchallenge.shared.infrastructure.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID
import kotlin.system.measureNanoTime

@Component
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val correlationId = request.getHeader(HEADER_NAME)
            ?.takeIf(::isValidCorrelationId)
            ?: UUID.randomUUID().toString()
        request.setAttribute(ATTRIBUTE_NAME, correlationId)
        response.setHeader(HEADER_NAME, correlationId)

        val elapsedNanos = measureNanoTime {
            MDC.put(MDC_KEY, correlationId)
            try {
                filterChain.doFilter(request, response)
            } finally {
                MDC.remove(MDC_KEY)
            }
        }

        requestLogger.info(
            "http_request method={} path={} status={} durationMs={}",
            request.method,
            sanitizePath(request.requestURI),
            response.status,
            elapsedNanos / NANOS_PER_MILLISECOND,
        )
    }

    private fun isValidCorrelationId(value: String): Boolean = CORRELATION_ID_PATTERN.matches(value)

    private fun sanitizePath(path: String): String =
        path
            .replace(DOCUMENT_PATTERN, "{document}")
            .replace(JWT_LIKE_PATTERN, "{token}")

    companion object {
        const val HEADER_NAME = "X-Correlation-ID"
        const val ATTRIBUTE_NAME = "garageFlow.correlationId"
        private const val MDC_KEY = "correlationId"
        private const val NANOS_PER_MILLISECOND = 1_000_000
        private val CORRELATION_ID_PATTERN = Regex("^[A-Za-z0-9._-]{1,100}$")
        private val DOCUMENT_PATTERN = Regex("\\b\\d{11}(?:\\d{3})?\\b")
        private val JWT_LIKE_PATTERN = Regex("\\b[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b")
        private val requestLogger = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
    }
}

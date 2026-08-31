package com.example.techchallenge.shared.api

import com.example.techchallenge.shared.domain.DomainValidationException
import com.example.techchallenge.shared.domain.ErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest

class PageRequestDto private constructor(
    val page: Int,
    val size: Int,
) {
    fun toSpringPageRequest(): PageRequest = PageRequest.of(page, size)

    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 100

        fun of(
            page: Int = DEFAULT_PAGE,
            size: Int = DEFAULT_SIZE,
        ): PageRequestDto {
            if (page < 0 || size !in 1..MAX_SIZE) {
                throw DomainValidationException(
                    ErrorCode.INVALID_PAGINATION,
                    "Page must be non-negative and size must be between 1 and $MAX_SIZE",
                )
            }
            return PageRequestDto(page, size)
        }
    }
}

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

fun <T, R> Page<T>.toPageResponse(transform: (T) -> R): PageResponse<R> = PageResponse(
    content = content.map(transform),
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)

package com.example.techchallenge.shared.domain

import java.time.Instant

fun interface ClockProvider {
    fun now(): Instant
}

fun interface SecureTokenGenerator {
    fun generate(byteLength: Int): String
}

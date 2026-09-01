package com.example.techchallenge.shared.api

import com.example.techchallenge.support.PostgreSqlIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
@TestPropertySource(properties = ["springdoc.api-docs.enabled=true"])
class OpenApiContractIntegrationTest : PostgreSqlIntegrationTest() {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @Test
    fun `publishes OpenAPI metadata bearer scheme and representative contract paths`() {
        val body = mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.components.securitySchemes.bearerAuth.scheme") { value("bearer") }
        }.andReturn().response.contentAsString
        val root = objectMapper.readTree(body)

        assertTrue(root.at("/paths/~1api~1v1~1auth~1token/post").isObject)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1customers/get/security/0/bearerAuth").isArray)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1customers/post/security/0/bearerAuth").isArray)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1vehicles/get/security/0/bearerAuth").isArray)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1services/post/security/0/bearerAuth").isArray)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1inventory-items/post/security/0/bearerAuth").isArray)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1service-orders/post/security/0/bearerAuth").isArray)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1service-orders~1{serviceOrderId}~1execution~1start/post/security/0/bearerAuth").isArray)
        assertTrue(root.at("/paths/~1api~1v1~1admin~1metrics~1execution-time/get/security/0/bearerAuth").isArray)
        assertFalse(root.at("/paths/~1api~1v1~1tracking~1service-orders~1{serviceOrderId}/get").has("security"))
        assertFalse(root.at("/paths/~1api~1v1~1customer-approvals~1{serviceOrderId}~1approve/post").has("security"))
    }
}

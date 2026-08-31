package com.example.techchallenge

import com.example.techchallenge.authentication.infrastructure.persistence.SpringDataAdministratorRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    classes = [TechChallengeApplication::class],
    properties = [
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "garage-flow.security.jwt.secret=01234567890123456789012345678901",
    ],
)
class TechChallengeApplicationTests {
    @MockitoBean
    private lateinit var administratorRepository: SpringDataAdministratorRepository

    @Test
    fun contextLoads() = Unit
}

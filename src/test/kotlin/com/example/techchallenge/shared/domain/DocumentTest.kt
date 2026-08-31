package com.example.techchallenge.shared.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DocumentTest {
    @ParameterizedTest
    @ValueSource(strings = ["529.982.247-25", "52998224725"])
    fun `normalizes and validates CPF`(raw: String) {
        val document = Document.from(raw)

        assertThat(document).isInstanceOf(Cpf::class.java)
        assertThat(document.value).isEqualTo("52998224725")
        assertThat(document.masked()).isEqualTo("***.982.247-**")
    }

    @ParameterizedTest
    @ValueSource(strings = ["04.252.011/0001-10", "04252011000110"])
    fun `normalizes and validates CNPJ`(raw: String) {
        val document = Document.from(raw)

        assertThat(document).isInstanceOf(Cnpj::class.java)
        assertThat(document.value).isEqualTo("04252011000110")
        assertThat(document.masked()).isEqualTo("**.252.011/0001-**")
    }

    @ParameterizedTest
    @ValueSource(strings = ["111.111.111-11", "529.982.247-24", "00.000.000/0000-00", "04.252.011/0001-11", "abc52998224725", "123"])
    fun `rejects invalid or repeated-digit documents`(raw: String) {
        assertThatThrownBy { Document.from(raw) }
            .isInstanceOf(DomainValidationException::class.java)
            .hasMessageContaining("document")
    }
}

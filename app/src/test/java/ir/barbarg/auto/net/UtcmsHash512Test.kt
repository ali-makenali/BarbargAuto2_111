package ir.barbarg.auto.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtcmsHash512Test {
    @Test
    fun matchesFixedReferenceVectorFromDocumentedAlgorithm() {
        val result = UtcmsHash512.calculate(
            companyCode = "TEST-COMPANY",
            password = "TestPass-123",
            salt = 123456789
        )

        assertEquals(123456789, result.salt)
        assertEquals(
            "BGfznOKIcPS7RyePXeDJjDhgFD7GJYoIip90QAqRHgwRIBAeInM1T0+lfMQMzYH7HEZ6OMYl6stFFrrRbfh/pQ==",
            result.hashedValue
        )
    }

    @Test
    fun generatedSaltIsNonNegative() {
        repeat(20) {
            assertTrue(UtcmsHash512.generateSalt() >= 0)
        }
    }
}

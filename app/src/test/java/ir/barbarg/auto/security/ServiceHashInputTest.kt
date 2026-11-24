package ir.barbarg.auto.security

import org.junit.Assert.assertThrows
import org.junit.Test

class ServiceHashInputTest {
    @Test
    fun blankCompanyCodeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ServiceHash.create(" ", "secret".toCharArray())
        }
    }

    @Test
    fun whitespaceOnlyPasswordIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ServiceHash.create("COMPANY", "   ".toCharArray())
        }
    }
}

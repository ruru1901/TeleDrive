package com.teledrive.android.telegram

import org.junit.Assert.assertEquals
import org.junit.Test

class TdLibTelegramGatewayTest {
    @Test
    fun reportsAvailabilityFromClasspath() {
        assertEquals(TdLibReflection.availableOrNull() != null, TdLibTelegramGateway.isAvailable())
    }
}

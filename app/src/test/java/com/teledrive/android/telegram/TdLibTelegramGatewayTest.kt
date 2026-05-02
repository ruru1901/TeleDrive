package com.teledrive.android.telegram

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TdLibTelegramGatewayTest {
    @Test
    fun reportsAvailableWhenTdLibClassesArePackaged() {
        assertTrue(TdLibTelegramGateway.isAvailable())
        assertNotNull(TdLibReflection.availableOrNull())
    }
}

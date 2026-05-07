package com.teledrive.android.telegram

import org.junit.Assert.assertTrue
import org.junit.Test

class TdLibTelegramGatewayTest {
    @Test
    fun reportsUnavailableOnHeadlessJvmWithoutNativeTdLib() {
        assertTrue("TDLib native libs should not be present in a headless JVM unit test", !TdLibTelegramGateway.isAvailable())
    }
}

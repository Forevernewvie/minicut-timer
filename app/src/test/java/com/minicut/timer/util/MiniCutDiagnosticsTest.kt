package com.minicut.timer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniCutDiagnosticsTest {
    @Test
    fun guard_returnsSuccessWithoutCallingReporter() {
        var reporterCalls = 0

        val result =
            MiniCutDiagnostics.guard("success-case", reporter = { _, _ -> reporterCalls += 1 }) {
                7
            }

        assertTrue(result.isSuccess)
        assertEquals(7, result.getOrNull())
        assertEquals(0, reporterCalls)
    }

    @Test
    fun guard_reportsAndReturnsFailure() {
        var reportedScope: String? = null
        var reportedMessage: String? = null

        val result =
            MiniCutDiagnostics.guard("failure-case", reporter = { scope, throwable ->
                reportedScope = scope
                reportedMessage = throwable.message
            }) {
                error("boom")
            }

        assertTrue(result.isFailure)
        assertEquals("failure-case", reportedScope)
        assertEquals("boom", reportedMessage)
    }
}

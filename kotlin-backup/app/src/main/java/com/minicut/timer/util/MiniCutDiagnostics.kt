package com.minicut.timer.util

typealias MiniCutFailureReporter = (scope: String, throwable: Throwable) -> Unit

object MiniCutDiagnostics {
    fun report(
        scope: String,
        throwable: Throwable,
    ) {
        System.err.println("MiniCut stability guard failed in $scope: ${throwable.message}")
        throwable.printStackTrace()
    }

    inline fun <T> guard(
        scope: String,
        reporter: MiniCutFailureReporter = ::report,
        block: () -> T,
    ): Result<T> =
        runCatching(block).onFailure { reporter(scope, it) }
}

package com.system.sysstatus.data

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class LogStatus { OK, NO_DATA, FAILED }

data class LogEntry(
    val id: Long,
    val timeMs: Long,
    // Collector that made the request (Battery / Memory)
    val source: String,
    val call: String,
    // Raw value exactly as returned by Android, before any conversion
    val result: String,
    val status: LogStatus,
    val micros: Long
)

data class LogTotals(
    val total: Long = 0,
    val ok: Long = 0,
    val noData: Long = 0,
    val failed: Long = 0
)

object RequestLog {

    private const val MAX_ENTRIES = 400
    private val nextId = AtomicLong(1)

    // Requests are recorded only while the Log screen is open
    @Volatile
    var enabled: Boolean = false

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries

    private val _totals = MutableStateFlow(LogTotals())
    val totals: StateFlow<LogTotals> = _totals

    fun newId(): Long = nextId.getAndIncrement()

    fun addAll(batch: List<LogEntry>) {
        if (batch.isEmpty()) return
        // Newest entry first
        _entries.update { old -> (batch.asReversed() + old).take(MAX_ENTRIES) }
        _totals.update { t ->
            t.copy(
                total = t.total + batch.size,
                ok = t.ok + batch.count { it.status == LogStatus.OK },
                noData = t.noData + batch.count { it.status == LogStatus.NO_DATA },
                failed = t.failed + batch.count { it.status == LogStatus.FAILED }
            )
        }
    }

    fun clear() {
        _entries.value = emptyList()
        _totals.value = LogTotals()
    }
}

// Collects the log entries of one polling pass and publishes them together
class ReadBatch(private val source: String) {

    private val items = ArrayList<LogEntry>()

    fun <T> trace(
        call: String,
        fallback: T,
        noData: (T) -> Boolean = { false },
        show: (T) -> String = { it.toString() },
        block: () -> T
    ): T {
        if (!RequestLog.enabled) {
            // Not recording: run the call without timing or formatting overhead
            return try { block() } catch (e: Exception) { fallback }
        }
        val start = System.nanoTime()
        return try {
            val value = block()
            val status = if (noData(value)) LogStatus.NO_DATA else LogStatus.OK
            add(call, show(value), status, start)
            value
        } catch (e: Exception) {
            add(call, "${e.javaClass.simpleName}: ${e.message.orEmpty()}", LogStatus.FAILED, start)
            fallback
        }
    }

    fun commit() {
        RequestLog.addAll(items)
        items.clear()
    }

    private fun add(call: String, result: String, status: LogStatus, startNanos: Long) {
        items.add(
            LogEntry(
                id = RequestLog.newId(),
                timeMs = System.currentTimeMillis(),
                source = source,
                call = call,
                result = result,
                status = status,
                micros = (System.nanoTime() - startNanos) / 1000
            )
        )
    }
}

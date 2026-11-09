package org.rsmod.content.other.bots

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import org.rsmod.api.random.GameRandom

/**
 * Keeps a pool of OSRS-style names fetched from the genr8rs name generator on a background thread.
 * The game thread only ever polls the pool, so a slow or unreachable site costs nothing: an empty
 * pool falls back to [BotLooks.name].
 */
@Singleton
class BotNames @Inject constructor(private val random: GameRandom) {
    private val pool = ConcurrentLinkedQueue<String>()
    private val client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build()

    @Volatile private var started = false

    fun next(taken: Set<String>): String {
        while (true) {
            val name = pool.poll() ?: break
            if (name.lowercase() !in taken) {
                return name
            }
        }
        return BotLooks.name(random, taken)
    }

    fun startFetching() {
        if (started) {
            return
        }
        started = true
        Thread(::fetchLoop, "bot-names").apply { isDaemon = true }.start()
    }

    private fun fetchLoop() {
        var failures = 0
        while (true) {
            if (pool.size >= POOL_TARGET) {
                Thread.sleep(IDLE_MILLIS)
                continue
            }
            val name = runCatching(::fetch).getOrNull()
            if (name != null) {
                failures = 0
                pool += name
                Thread.sleep(REQUEST_GAP_MILLIS)
                continue
            }
            if (++failures == FAILURES_BEFORE_BACKOFF) {
                logger.info { "Bot name generator unreachable; using built-in names for now." }
            }
            val wait = if (failures >= FAILURES_BEFORE_BACKOFF) BACKOFF_MILLIS else RETRY_MILLIS
            Thread.sleep(wait)
        }
    }

    private fun fetch(): String? {
        val request =
            HttpRequest.newBuilder(URI.create(API_URL))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != HTTP_OK) {
            return null
        }
        val name = RESULT.find(response.body())?.groupValues?.get(1) ?: return null
        return name.takeIf(VALID_NAME::matches)
    }

    private companion object {
        private val logger = InlineLogger()

        private const val API_URL =
            "https://genr8rs.com/api/Content/OldSchoolRunescape/NameGenerator" +
                "?_sAccountType=standard"
        private const val USER_AGENT = "OpenRune-Server (cosmetic bot names)"
        private val RESULT = Regex("\"_sResult\"\\s*:\\s*\"([^\"]+)\"")
        private val VALID_NAME = Regex("[A-Za-z0-9 _-]{1,12}")

        private const val HTTP_OK = 200
        private const val TIMEOUT_SECONDS = 5L
        private const val POOL_TARGET = 100
        private const val REQUEST_GAP_MILLIS = 1_000L
        private const val RETRY_MILLIS = 10_000L
        private const val IDLE_MILLIS = 30_000L
        private const val BACKOFF_MILLIS = 600_000L
        private const val FAILURES_BEFORE_BACKOFF = 3
    }
}

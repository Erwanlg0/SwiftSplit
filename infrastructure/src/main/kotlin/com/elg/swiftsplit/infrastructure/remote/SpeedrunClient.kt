package com.elg.swiftsplit.infrastructure.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SpeedrunGameResponse(val data: List<SpeedrunGame>)

@Serializable
data class SpeedrunGame(val id: String, val names: SpeedrunGameNames)

@Serializable
data class SpeedrunGameNames(val international: String)

@Serializable
data class SpeedrunCategoryResponse(val data: List<SpeedrunCategory>)

@Serializable
data class SpeedrunCategory(val id: String, val name: String)

@Serializable
data class SpeedrunLeaderboardResponse(val data: SpeedrunLeaderboardData)

@Serializable
data class SpeedrunLeaderboardData(val runs: List<SpeedrunRunPlacement>)

@Serializable
data class SpeedrunRunPlacement(val place: Int, val run: SpeedrunRun)

@Serializable
data class SpeedrunRun(val id: String, val times: SpeedrunTimes, val splits: SpeedrunSplits? = null)

@Serializable
data class SpeedrunTimes(val primary_t: Double)

@Serializable
data class SpeedrunSplits(val uri: String)

@Serializable
data class SpeedrunSingleRunResponse(val data: SpeedrunRun)

@Singleton
class SpeedrunClient @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun getRequest(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "SwiftSplit-Mobile/1.0")

        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            throw Exception("HTTP Error: $code")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        conn.disconnect()
        return response.toString()
    }

    suspend fun searchGames(query: String): List<SpeedrunGame> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.speedrun.com/api/v1/games?name=$encodedQuery&limit=10"
        val responseStr = getRequest(url)
        val parsed = json.decodeFromString<SpeedrunGameResponse>(responseStr)
        return parsed.data
    }

    suspend fun getCategories(gameId: String): List<SpeedrunCategory> {
        val url = "https://www.speedrun.com/api/v1/games/$gameId/categories"
        val responseStr = getRequest(url)
        val parsed = json.decodeFromString<SpeedrunCategoryResponse>(responseStr)
        // Only return standard run categories (type == "per-game" is usually standard, but simple list is fine)
        return parsed.data
    }

    suspend fun getLeaderboard(gameId: String, categoryId: String): List<SpeedrunRunPlacement> {
        val url = "https://www.speedrun.com/api/v1/leaderboards/$gameId/category/$categoryId?embed=runs"
        val responseStr = getRequest(url)
        val parsed = json.decodeFromString<SpeedrunLeaderboardResponse>(responseStr)
        return parsed.data.runs
    }

    suspend fun getRun(runId: String): SpeedrunRun {
        val url = "https://www.speedrun.com/api/v1/runs/$runId"
        val responseStr = getRequest(url)
        val parsed = json.decodeFromString<SpeedrunSingleRunResponse>(responseStr)
        return parsed.data
    }
}

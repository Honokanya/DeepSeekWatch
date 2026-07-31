package com.honoka.deepseekwatch.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class BalanceInfo(
    val currency: String,
    @SerialName("total_balance") val totalBalance: String,
    @SerialName("granted_balance") val grantedBalance: String,
    @SerialName("topped_up_balance") val toppedUpBalance: String,
)

@Serializable
data class BalanceResponse(
    @SerialName("is_available") val isAvailable: Boolean,
    @SerialName("balance_infos") val balanceInfos: List<BalanceInfo> = emptyList(),
)

class DeepSeekApiException(val code: Int, val responseBody: String) :
    Exception("DeepSeek API error HTTP $code: $responseBody")

class DeepSeekApi(private val baseUrl: String = "https://api.deepseek.com") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchBalance(apiKey: String): BalanceResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/user/balance")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw DeepSeekApiException(resp.code, body)
            json.decodeFromString<BalanceResponse>(body)
        }
    }
}

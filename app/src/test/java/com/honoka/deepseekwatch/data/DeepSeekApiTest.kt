package com.honoka.deepseekwatch.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class DeepSeekApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: DeepSeekApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = DeepSeekApi(baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchBalance parses valid response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"110.00","granted_balance":"10.00","topped_up_balance":"100.00"}]}"""
        ))
        val resp = api.fetchBalance("sk-test")
        assertTrue(resp.isAvailable)
        assertEquals("110.00", resp.balanceInfos.first().totalBalance)
        assertEquals("10.00", resp.balanceInfos.first().grantedBalance)
        assertEquals("CNY", resp.balanceInfos.first().currency)
    }

    @Test
    fun `fetchBalance throws DeepSeekApiException on 401`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))
        val ex = assertFailsWith<DeepSeekApiException> { api.fetchBalance("bad-key") }
        assertEquals(401, ex.code)
    }

    @Test
    fun `fetchBalance throws on network error`() = runBlocking {
        server.shutdown() // 连接拒绝
        val ex = assertFailsWith<Exception> { api.fetchBalance("sk-x") }
        assertTrue(ex.message.orEmpty().contains("Failed to connect", ignoreCase = true))
    }
}

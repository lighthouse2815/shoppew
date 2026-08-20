package com.shoppew.android.data

import com.shoppew.android.core.api.Category
import com.shoppew.android.core.api.PageResponse
import com.shoppew.android.core.api.ProductDetail
import com.shoppew.android.core.api.ProductSummary
import com.shoppew.android.core.api.ShoppewApi
import com.shoppew.android.core.network.AccessTokenStore
import com.shoppew.android.core.network.RefreshSession
import com.shoppew.android.core.push.PushInstallation
import com.shoppew.android.data.local.CatalogCache
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RealShoppewRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: AccessTokenStore
    private lateinit var session: TestRefreshSession
    private lateinit var cache: MemoryCatalogCache
    private lateinit var pushInstallation: TestPushInstallation
    private lateinit var repository: RealShoppewRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokens = AccessTokenStore()
        session = TestRefreshSession()
        cache = MemoryCatalogCache()
        pushInstallation = TestPushInstallation()
        val api = createApi(server)
        repository = RealShoppewRepository(api, api, tokens, session, cache, pushInstallation)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    @Test
    fun `login trims email parses envelope and keeps access token in memory`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {"success":true,"data":{"accessToken":"access-123","user":{"id":"user-1","email":"buyer@example.test","displayName":"Người mua","roles":["CUSTOMER"],"status":"ACTIVE","emailVerified":true}}}
                """.trimIndent(),
            ),
        )

        val result = repository.login("  buyer@example.test  ", "correct-password")

        assertTrue(result.isSuccess)
        assertEquals("user-1", result.getOrThrow().id)
        assertEquals("access-123", tokens.value)
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("/api/v1/auth/login", request!!.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"email\":\"buyer@example.test\""))
        assertFalse(body.contains("  buyer@example.test  "))
        assertTrue(body.contains("\"deviceName\":\"shoppew Android\""))
    }

    @Test
    fun `validation error envelope preserves stable code and field errors`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {"success":false,"error":{"code":"VALIDATION_ERROR","message":"Dữ liệu chưa hợp lệ","details":[{"field":"email","code":"INVALID","message":"Email chưa đúng định dạng"}]}}
                """.trimIndent(),
                status = 422,
            ),
        )

        val error = repository.login("invalid", "password").exceptionOrNull() as ShoppewException

        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals("Dữ liệu chưa hợp lệ", error.message)
        assertEquals("Email chưa đúng định dạng", error.fieldErrors["email"])
        assertNull(tokens.value)
    }

    @Test
    fun `authenticated session registers FID and logout revokes it before the session`() = runTest {
        pushInstallation.target = "firebase-installation-id-shoppew-test-2026"
        server.enqueue(
            jsonResponse(
                """
                {"success":true,"data":{"accessToken":"access-push","user":{"id":"user-push","email":"push@example.test","displayName":"Push User","roles":["CUSTOMER"],"status":"ACTIVE","emailVerified":true}}}
                """.trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                """
                {"success":true,"data":{"id":"device-1","platform":"ANDROID","targetType":"FID","active":true}}
                """.trimIndent(),
            ),
        )
        server.enqueue(jsonResponse("""{"success":true,"data":{"revoked":true}}"""))
        server.enqueue(jsonResponse("""{"success":true,"data":{"loggedOut":true}}"""))

        assertTrue(repository.login("push@example.test", "correct-password").isSuccess)
        val login = server.takeRequest(1, TimeUnit.SECONDS)
        val registration = server.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("/api/v1/auth/login", login?.path)
        assertEquals("/api/v1/notifications/devices/current", registration?.path)
        val registrationBody = registration!!.body.readUtf8()
        assertTrue(registrationBody.contains("\"targetType\":\"FID\""))
        assertTrue(registrationBody.contains("firebase-installation-id-shoppew-test-2026"))

        repository.logout()

        val revocation = server.takeRequest(1, TimeUnit.SECONDS)
        val logout = server.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("/api/v1/notifications/devices/current", revocation?.path)
        assertTrue(revocation!!.body.readUtf8().contains("firebase-installation-id-shoppew-test-2026"))
        assertEquals("/api/v1/auth/logout", logout?.path)
        assertNull(tokens.value)
        assertTrue(session.cleared)
    }

    @Test
    fun `product page is cached and used only after a network failure`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {"success":true,"data":{"content":[{"id":"product-1","shopId":"shop-1","shopName":"Góc Việt","categoryId":"cat-1","categoryName":"Thời trang","name":"Áo cotton","slug":"ao-cotton","status":"ACTIVE","minimumPrice":"125000","currency":"VND"}],"page":0,"size":24,"totalElements":1,"totalPages":1}}
                """.trimIndent(),
            ),
        )
        val online = repository.products("  Áo  ", "cat-1", 0)
        assertEquals("product-1", online.getOrThrow().content.single().id)
        assertEquals(listOf("Áo"), cache.searches)
        assertEquals("product-1", cache.products.single().id)

        server.shutdown()
        val offline = repository.products("Áo", "cat-1", 0)

        assertTrue(offline.isSuccess)
        assertEquals("product-1", offline.getOrThrow().content.single().id)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `network failure without cached data stays a recoverable network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = repository.product("chua-tung-xem").exceptionOrNull() as ShoppewException

        assertEquals("NETWORK_ERROR", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `restore rejects missing secure refresh session without calling server`() = runTest {
        session.present = false

        val error = repository.restoreSession().exceptionOrNull() as ShoppewException

        assertEquals("VALIDATION", error.code)
        assertEquals(0, server.requestCount)
    }

    private fun createApi(server: MockWebServer): ShoppewApi {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true; encodeDefaults = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ShoppewApi::class.java)
    }

    private fun jsonResponse(body: String, status: Int = 200) = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

private class TestRefreshSession(var present: Boolean = false) : RefreshSession {
    var cleared = false
    override fun hasRefreshSession(): Boolean = present
    override fun clear() {
        present = false
        cleared = true
    }
}

private class TestPushInstallation(var target: String? = null) : PushInstallation {
    override suspend fun current(): String? = target
    override fun cached(): String? = target
}

private class MemoryCatalogCache : CatalogCache {
    var categoryItems: List<Category>? = null
    var products: List<ProductSummary> = emptyList()
    val details = mutableMapOf<String, ProductDetail>()
    val searches = mutableListOf<String>()
    val viewed = mutableListOf<ProductSummary>()

    override suspend fun saveCategories(categories: List<Category>) {
        categoryItems = categories
    }

    override suspend fun categories(): List<Category>? = categoryItems

    override suspend fun saveProducts(products: List<ProductSummary>) {
        this.products = (this.products + products).distinctBy(ProductSummary::id)
    }

    override suspend fun products(query: String?, categoryId: String?, page: Int, size: Int): PageResponse<ProductSummary>? {
        val filtered = products.filter { product ->
            (categoryId == null || product.categoryId == categoryId) &&
                (query.isNullOrBlank() || product.name.contains(query, ignoreCase = true))
        }
        if (filtered.isEmpty()) return null
        val content = filtered.drop(page * size).take(size)
        if (content.isEmpty()) return null
        return PageResponse(content, page, size, filtered.size.toLong(), (filtered.size + size - 1) / size)
    }

    override suspend fun saveProduct(product: ProductDetail) {
        details[product.slug] = product
    }

    override suspend fun product(slug: String): ProductDetail? = details[slug]

    override suspend fun rememberSearch(query: String) {
        searches.remove(query)
        searches.add(0, query)
    }

    override suspend fun recentSearches(limit: Int): List<String> = searches.take(limit)
    override suspend fun clearSearches() = searches.clear()

    override suspend fun rememberViewed(product: ProductDetail) {
        viewed.removeAll { it.id == product.id }
        viewed.add(0, ProductSummary(id = product.id, name = product.name, slug = product.slug, categoryId = product.categoryId))
    }

    override suspend fun recentlyViewed(limit: Int): List<ProductSummary> = viewed.take(limit)
}

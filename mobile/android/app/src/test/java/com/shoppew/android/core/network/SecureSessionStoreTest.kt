package com.shoppew.android.core.network

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureSessionStoreTest {
    @Test
    fun `access token remains memory only and can be revoked`() {
        val store = AccessTokenStore()
        assertNull(store.value)

        store.update("access-token")
        assertEquals("access-token", store.value)

        store.clear()
        assertNull(store.value)
    }

    @Test
    fun `refresh cookie jar persists only refresh cookie and scopes it to matching host`() {
        val store = FakeCookieStore()
        val jar = EncryptedRefreshCookieJar(store)
        val apiUrl = "https://api.shoppew.vn/api/v1/auth/refresh".toHttpUrl()
        val refresh = Cookie.Builder()
            .name("shoppew_refresh")
            .value("opaque-session")
            .hostOnlyDomain("api.shoppew.vn")
            .path("/")
            .httpOnly()
            .secure()
            .expiresAt(System.currentTimeMillis() + 60_000)
            .build()
        val unrelated = Cookie.Builder()
            .name("tracking")
            .value("ignored")
            .hostOnlyDomain("api.shoppew.vn")
            .path("/")
            .expiresAt(System.currentTimeMillis() + 60_000)
            .build()

        jar.saveFromResponse(apiUrl, listOf(unrelated, refresh))

        assertEquals(refresh, store.cookie)
        assertTrue(jar.hasRefreshSession())
        assertEquals(listOf(refresh), jar.loadForRequest(apiUrl))
        assertTrue(jar.loadForRequest("https://other.example/api".toHttpUrl()).isEmpty())
    }

    @Test
    fun `expired refresh response clears the secure session`() {
        val store = FakeCookieStore()
        val jar = EncryptedRefreshCookieJar(store)
        val url = "https://api.shoppew.vn/".toHttpUrl()
        store.cookie = Cookie.Builder()
            .name("shoppew_refresh")
            .value("old")
            .hostOnlyDomain("api.shoppew.vn")
            .path("/")
            .expiresAt(System.currentTimeMillis() + 60_000)
            .build()
        val deletionCookie = Cookie.Builder()
            .name("shoppew_refresh")
            .value("")
            .hostOnlyDomain("api.shoppew.vn")
            .path("/")
            .expiresAt(0)
            .build()

        jar.saveFromResponse(url, listOf(deletionCookie))

        assertFalse(jar.hasRefreshSession())
        assertTrue(store.cleared)
    }
}

private class FakeCookieStore : SecureCookieStore {
    var cookie: Cookie? = null
    var cleared = false
    override fun save(cookie: Cookie) {
        this.cookie = cookie
    }

    override fun load(): Cookie? = cookie

    override fun clear() {
        cookie = null
        cleared = true
    }
}

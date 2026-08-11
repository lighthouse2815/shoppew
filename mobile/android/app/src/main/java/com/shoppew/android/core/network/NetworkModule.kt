package com.shoppew.android.core.network

import android.content.Context
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.shoppew.android.BuildConfig
import com.shoppew.android.core.api.ApiEnvelope
import com.shoppew.android.core.api.AuthResponse
import com.shoppew.android.core.api.ShoppewApi
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.POST

private interface RefreshApi {
    @POST("api/v1/auth/refresh")
    fun refreshBlocking(): Call<ApiEnvelope<AuthResponse>>
}

class NetworkModule(context: Context) {
    val accessTokens = AccessTokenStore()
    val cookieJar = EncryptedRefreshCookieJar(SecureSessionStore(context))

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }
    private val converter = json.asConverterFactory("application/json".toMediaType())
    private val logger = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }
    private val publicClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logger)
        .build()
    private val refreshApi = retrofit(publicClient).create(RefreshApi::class.java)
    private val tokenAuthenticator = RefreshAuthenticator(refreshApi, accessTokens, cookieJar)
    private val authenticatedClient = publicClient.newBuilder()
        .addInterceptor(AccessTokenInterceptor(accessTokens))
        .authenticator(tokenAuthenticator)
        .build()

    val publicApi: ShoppewApi = retrofit(publicClient).create(ShoppewApi::class.java)
    val authenticatedApi: ShoppewApi = retrofit(authenticatedClient).create(ShoppewApi::class.java)

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(converter)
        .build()
}

private class AccessTokenInterceptor(private val tokens: AccessTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.value
        val request = if (token.isNullOrBlank()) chain.request() else chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

private class RefreshAuthenticator(
    private val refreshApi: RefreshApi,
    private val tokens: AccessTokenStore,
    private val cookieJar: EncryptedRefreshCookieJar,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith("/auth/refresh") || responseCount(response) >= 2) return null
        synchronized(lock) {
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val currentToken = tokens.value
            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return response.request.newBuilder().header("Authorization", "Bearer $currentToken").build()
            }
            if (!cookieJar.hasRefreshSession()) return null
            val refreshed = runCatching { refreshApi.refreshBlocking().execute() }.getOrNull()
            val body = refreshed?.body()
            val token = body?.takeIf { refreshed.isSuccessful && it.success }?.data?.accessToken
            if (token.isNullOrBlank()) {
                tokens.clear()
                cookieJar.clear()
                return null
            }
            tokens.update(token)
            return response.request.newBuilder().header("Authorization", "Bearer $token").build()
        }
    }

    private fun responseCount(response: Response): Int {
        var current: Response? = response
        var count = 0
        while (current != null) {
            count += 1
            current = current.priorResponse
        }
        return count
    }
}

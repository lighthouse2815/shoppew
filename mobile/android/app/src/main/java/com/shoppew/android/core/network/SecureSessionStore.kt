package com.shoppew.android.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class AccessTokenStore {
    @Volatile
    var value: String? = null
        private set

    fun update(token: String?) {
        value = token
    }

    fun clear() = update(null)
}

interface SecureCookieStore {
    fun save(cookie: Cookie)
    fun load(): Cookie?
    fun clear()
}

class SecureSessionStore(context: Context) : SecureCookieStore {
    private val preferences = context.getSharedPreferences("secure_session", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun save(cookie: Cookie) {
        val serialized = listOf(
            cookie.name,
            cookie.value,
            cookie.domain,
            cookie.path,
            cookie.expiresAt.toString(),
            cookie.secure.toString(),
            cookie.httpOnly.toString(),
            cookie.hostOnly.toString(),
        ).joinToString(SEPARATOR)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(serialized.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString(KEY_COOKIE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    override fun load(): Cookie? {
        val encrypted = preferences.getString(KEY_COOKIE, null) ?: return null
        val iv = preferences.getString(KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            val serialized = String(
                cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)),
                StandardCharsets.UTF_8,
            )
            val values = serialized.split(SEPARATOR)
            require(values.size == 8)
            val builder = Cookie.Builder()
                .name(values[0])
                .value(values[1])
                .path(values[3])
                .expiresAt(values[4].toLong())
            if (values[7].toBoolean()) builder.hostOnlyDomain(values[2]) else builder.domain(values[2])
            if (values[5].toBoolean()) builder.secure()
            if (values[6].toBoolean()) builder.httpOnly()
            builder.build().takeUnless { it.expiresAt <= System.currentTimeMillis() }
        }.getOrElse {
            clear()
            null
        }
    }

    override fun clear() {
        preferences.edit().remove(KEY_COOKIE).remove(KEY_IV).apply()
    }

    private fun secretKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "shoppew_refresh_cookie_key"
        const val KEY_COOKIE = "refresh_cookie"
        const val KEY_IV = "refresh_cookie_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = "\u001F"
    }
}

interface RefreshSession {
    fun hasRefreshSession(): Boolean
    fun clear()
}

class EncryptedRefreshCookieJar(private val sessionStore: SecureCookieStore) : CookieJar, RefreshSession {
    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.firstOrNull { it.name == REFRESH_COOKIE }?.let { cookie ->
            if (cookie.expiresAt <= System.currentTimeMillis()) sessionStore.clear() else sessionStore.save(cookie)
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookie = sessionStore.load() ?: return emptyList()
        return if (cookie.matches(url)) listOf(cookie) else emptyList()
    }

    override fun hasRefreshSession(): Boolean = sessionStore.load() != null

    override fun clear() = sessionStore.clear()

    private companion object {
        const val REFRESH_COOKIE = "shoppew_refresh"
    }
}

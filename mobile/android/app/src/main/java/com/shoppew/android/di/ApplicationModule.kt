package com.shoppew.android.di

import android.content.Context
import androidx.room.Room
import com.shoppew.android.core.connectivity.AndroidConnectivityObserver
import com.shoppew.android.core.connectivity.ConnectivityObserver
import com.shoppew.android.core.network.NetworkModule
import com.shoppew.android.data.RealShoppewRepository
import com.shoppew.android.data.ShoppewRepository
import com.shoppew.android.data.local.CatalogCache
import com.shoppew.android.data.local.RoomCatalogCache
import com.shoppew.android.data.local.ShoppewDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun network(@ApplicationContext context: Context): NetworkModule = NetworkModule(context)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ShoppewDatabase = Room.databaseBuilder(
        context,
        ShoppewDatabase::class.java,
        "shoppew_browsing.db",
    ).build()

    @Provides
    @Singleton
    fun catalogCache(database: ShoppewDatabase, json: Json): CatalogCache =
        RoomCatalogCache(database.catalogCacheDao(), json)

    @Provides
    @Singleton
    fun repository(network: NetworkModule, cache: CatalogCache): ShoppewRepository = RealShoppewRepository(
        publicApi = network.publicApi,
        api = network.authenticatedApi,
        tokens = network.accessTokens,
        cookies = network.cookieJar,
        catalogCache = cache,
    )

    @Provides
    @Singleton
    fun connectivityObserver(implementation: AndroidConnectivityObserver): ConnectivityObserver = implementation
}

package com.shoppew.android.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppew.android.core.api.ProductSummary
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCatalogCacheTest {
    private lateinit var database: ShoppewDatabase
    private lateinit var cache: CatalogCache

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShoppewDatabase::class.java,
        ).allowMainThreadQueries().build()
        cache = RoomCatalogCache(database.catalogCacheDao(), Json { ignoreUnknownKeys = true; explicitNulls = false })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun productCacheFiltersByCategoryAndQuery() = runTest {
        cache.saveProducts(
            listOf(
                ProductSummary(id = "p-1", slug = "ao-cotton", name = "Áo cotton", categoryId = "fashion", shopName = "Góc Việt"),
                ProductSummary(id = "p-2", slug = "tai-nghe", name = "Tai nghe", categoryId = "electronics", shopName = "Âm thanh Việt"),
            ),
        )

        val fashion = cache.products("cotton", "fashion", page = 0, size = 24)

        assertEquals("p-1", fashion?.content?.single()?.id)
        assertNull(cache.products("không có", null, page = 0, size = 24))
    }

    @Test
    fun searchHistoryIsDeduplicatedAndMostRecentFirst() = runTest {
        cache.rememberSearch("áo cotton")
        cache.rememberSearch("tai nghe")
        cache.rememberSearch("  ÁO COTTON  ")

        assertEquals(listOf("ÁO COTTON", "tai nghe"), cache.recentSearches())
    }
}

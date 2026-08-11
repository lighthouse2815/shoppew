package com.shoppew.android.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.shoppew.android.core.api.Category
import com.shoppew.android.core.api.PageResponse
import com.shoppew.android.core.api.ProductDetail
import com.shoppew.android.core.api.ProductSummary
import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Read-only browsing cache. Checkout, cart, account, order and payment mutations never use this
 * store as an authority and are deliberately absent from this contract.
 */
interface CatalogCache {
    suspend fun saveCategories(categories: List<Category>)
    suspend fun categories(): List<Category>?
    suspend fun saveProducts(products: List<ProductSummary>)
    suspend fun products(query: String?, categoryId: String?, page: Int, size: Int): PageResponse<ProductSummary>?
    suspend fun saveProduct(product: ProductDetail)
    suspend fun product(slug: String): ProductDetail?
    suspend fun rememberSearch(query: String)
    suspend fun recentSearches(limit: Int = 8): List<String>
    suspend fun clearSearches()
    suspend fun rememberViewed(product: ProductDetail)
    suspend fun recentlyViewed(limit: Int = 8): List<ProductSummary>
}

@Entity(tableName = "cached_categories")
data class CachedCategoriesEntity(
    @PrimaryKey val cacheKey: String = ROOT_CATEGORY_CACHE_KEY,
    val payload: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "cached_product_summaries",
    indices = [Index(value = ["slug"], unique = true), Index(value = ["categoryId"])],
)
data class CachedProductSummaryEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val categoryId: String,
    val searchableText: String,
    val payload: String,
    val cachedAt: Long,
)

@Entity(tableName = "cached_product_details")
data class CachedProductDetailEntity(
    @PrimaryKey val slug: String,
    val productId: String,
    val payload: String,
    val cachedAt: Long,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val normalizedQuery: String,
    val displayQuery: String,
    val searchedAt: Long,
)

@Entity(tableName = "recently_viewed_products")
data class RecentlyViewedProductEntity(
    @PrimaryKey val productId: String,
    val slug: String,
    val payload: String,
    val viewedAt: Long,
)

@Dao
interface CatalogCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategories(entity: CachedCategoriesEntity)

    @Query("SELECT * FROM cached_categories WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun categories(cacheKey: String = ROOT_CATEGORY_CACHE_KEY): CachedCategoriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProducts(entities: List<CachedProductSummaryEntity>)

    @Query(
        """
        SELECT * FROM cached_product_summaries
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:query IS NULL OR searchableText LIKE '%' || :query || '%')
        ORDER BY cachedAt DESC, id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun products(query: String?, categoryId: String?, limit: Int, offset: Int): List<CachedProductSummaryEntity>

    @Query(
        """
        SELECT COUNT(*) FROM cached_product_summaries
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:query IS NULL OR searchableText LIKE '%' || :query || '%')
        """,
    )
    suspend fun productCount(query: String?, categoryId: String?): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProduct(entity: CachedProductDetailEntity)

    @Query("SELECT * FROM cached_product_details WHERE slug = :slug LIMIT 1")
    suspend fun product(slug: String): CachedProductDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun rememberSearch(entity: SearchHistoryEntity)

    @Query("SELECT displayQuery FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    suspend fun recentSearches(limit: Int): List<String>

    @Query("DELETE FROM search_history")
    suspend fun clearSearches()

    @Query("DELETE FROM search_history WHERE normalizedQuery NOT IN (SELECT normalizedQuery FROM search_history ORDER BY searchedAt DESC LIMIT :keep)")
    suspend fun trimSearches(keep: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun rememberViewed(entity: RecentlyViewedProductEntity)

    @Query("SELECT * FROM recently_viewed_products ORDER BY viewedAt DESC LIMIT :limit")
    suspend fun recentlyViewed(limit: Int): List<RecentlyViewedProductEntity>

    @Query("DELETE FROM recently_viewed_products WHERE productId NOT IN (SELECT productId FROM recently_viewed_products ORDER BY viewedAt DESC LIMIT :keep)")
    suspend fun trimRecentlyViewed(keep: Int)

    @Transaction
    suspend fun updateSearchHistory(entity: SearchHistoryEntity) {
        rememberSearch(entity)
        trimSearches(MAX_SEARCH_HISTORY)
    }

    @Transaction
    suspend fun updateRecentlyViewed(entity: RecentlyViewedProductEntity) {
        rememberViewed(entity)
        trimRecentlyViewed(MAX_RECENTLY_VIEWED)
    }
}

@Database(
    entities = [
        CachedCategoriesEntity::class,
        CachedProductSummaryEntity::class,
        CachedProductDetailEntity::class,
        SearchHistoryEntity::class,
        RecentlyViewedProductEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ShoppewDatabase : RoomDatabase() {
    abstract fun catalogCacheDao(): CatalogCacheDao
}

class RoomCatalogCache(
    private val dao: CatalogCacheDao,
    private val json: Json,
) : CatalogCache {
    override suspend fun saveCategories(categories: List<Category>) {
        dao.saveCategories(CachedCategoriesEntity(payload = json.encodeToString(categories), cachedAt = now()))
    }

    override suspend fun categories(): List<Category>? = dao.categories()?.payload?.let { payload ->
        runCatching { json.decodeFromString<List<Category>>(payload) }.getOrNull()
    }

    override suspend fun saveProducts(products: List<ProductSummary>) {
        if (products.isEmpty()) return
        val cachedAt = now()
        dao.saveProducts(products.map { product -> product.toCacheEntity(json, cachedAt) })
    }

    override suspend fun products(
        query: String?,
        categoryId: String?,
        page: Int,
        size: Int,
    ): PageResponse<ProductSummary>? {
        val normalizedQuery = query.normalizedSearch().takeIf(String::isNotEmpty)
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val entities = dao.products(normalizedQuery, categoryId, safeSize, safePage * safeSize)
        val products = entities.mapNotNull { entity ->
            runCatching { json.decodeFromString<ProductSummary>(entity.payload) }.getOrNull()
        }
        if (products.isEmpty()) return null
        val total = dao.productCount(normalizedQuery, categoryId)
        return PageResponse(
            content = products,
            page = safePage,
            size = safeSize,
            totalElements = total,
            totalPages = ((total + safeSize - 1) / safeSize).toInt(),
        )
    }

    override suspend fun saveProduct(product: ProductDetail) {
        dao.saveProduct(
            CachedProductDetailEntity(
                slug = product.slug,
                productId = product.id,
                payload = json.encodeToString(product),
                cachedAt = now(),
            ),
        )
    }

    override suspend fun product(slug: String): ProductDetail? = dao.product(slug)?.payload?.let { payload ->
        runCatching { json.decodeFromString<ProductDetail>(payload) }.getOrNull()
    }

    override suspend fun rememberSearch(query: String) {
        val display = query.trim().replace(Regex("\\s+"), " ").take(MAX_QUERY_LENGTH)
        val normalized = display.normalizedSearch()
        if (normalized.isEmpty()) return
        dao.updateSearchHistory(SearchHistoryEntity(normalized, display, now()))
    }

    override suspend fun recentSearches(limit: Int): List<String> = dao.recentSearches(limit.coerceIn(1, MAX_SEARCH_HISTORY))

    override suspend fun clearSearches() = dao.clearSearches()

    override suspend fun rememberViewed(product: ProductDetail) {
        val summary = product.toSummary()
        dao.saveProducts(listOf(summary.toCacheEntity(json, now())))
        dao.updateRecentlyViewed(
            RecentlyViewedProductEntity(
                productId = product.id,
                slug = product.slug,
                payload = json.encodeToString(summary),
                viewedAt = now(),
            ),
        )
    }

    override suspend fun recentlyViewed(limit: Int): List<ProductSummary> = dao.recentlyViewed(limit.coerceIn(1, MAX_RECENTLY_VIEWED))
        .mapNotNull { entity -> runCatching { json.decodeFromString<ProductSummary>(entity.payload) }.getOrNull() }

    private fun now(): Long = System.currentTimeMillis()
}

private fun ProductSummary.toCacheEntity(json: Json, cachedAt: Long) = CachedProductSummaryEntity(
    id = id,
    slug = slug,
    categoryId = categoryId,
    searchableText = listOf(name, shopName, categoryName, brandName.orEmpty()).joinToString(" ").normalizedSearch(),
    payload = json.encodeToString(this),
    cachedAt = cachedAt,
)

private fun ProductDetail.toSummary(): ProductSummary {
    val activeVariants = variants.filter { it.status == "ACTIVE" }
    val minimumVariant = activeVariants.minByOrNull { it.price }
    return ProductSummary(
        id = id,
        shopId = shopId,
        shopName = shopName,
        categoryId = categoryId,
        categoryName = categoryName,
        brandId = brandId,
        brandName = brandName,
        name = name,
        slug = slug,
        shortDescription = shortDescription,
        status = status,
        primaryImageUrl = images.firstOrNull { it.primary }?.url ?: images.minByOrNull { it.sortOrder }?.url,
        minimumPrice = minimumVariant?.price,
        originalMinimumPrice = minimumVariant?.compareAtPrice,
        currency = minimumVariant?.currency ?: "VND",
        ratingAverage = ratingAverage,
        reviewCount = reviewCount,
        soldCount = soldCount,
    )
}

private fun String?.normalizedSearch(): String = this.orEmpty().trim().lowercase(Locale.forLanguageTag("vi-VN"))

private const val ROOT_CATEGORY_CACHE_KEY = "root"
private const val MAX_QUERY_LENGTH = 120
private const val MAX_SEARCH_HISTORY = 20
private const val MAX_RECENTLY_VIEWED = 24

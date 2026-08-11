package com.shoppew.android.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppew.android.core.api.Category
import com.shoppew.android.core.api.ProductDetail
import com.shoppew.android.core.api.ProductSummary
import com.shoppew.android.core.api.Review
import com.shoppew.android.data.ShoppewRepository
import com.shoppew.android.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val categories: LoadState<List<Category>> = LoadState.Loading,
    val products: LoadState<List<ProductSummary>> = LoadState.Loading,
    val query: String = "",
    val selectedCategoryId: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val recentlyViewed: List<ProductSummary> = emptyList(),
)

data class ProductUiState(
    val detail: LoadState<ProductDetail> = LoadState.Idle,
    val reviews: LoadState<List<Review>> = LoadState.Idle,
)

@HiltViewModel
class CatalogViewModel @Inject constructor(private val repository: ShoppewRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()
    private val _productState = MutableStateFlow(ProductUiState())
    val productState: StateFlow<ProductUiState> = _productState.asStateFlow()

    init {
        loadLocalBrowsingActivity()
        loadCategories()
        loadProducts()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(categories = LoadState.Loading) }
            _uiState.update { state ->
                repository.categories().fold(
                    { state.copy(categories = LoadState.Success(it)) },
                    { state.copy(categories = LoadState.Error(it.message ?: "Không tải được danh mục")) },
                )
            }
        }
    }

    fun setQuery(value: String) = _uiState.update { it.copy(query = value) }

    fun search() = loadProducts(reset = true)

    fun useSearchSuggestion(value: String) {
        _uiState.update { it.copy(query = value) }
        loadProducts(reset = true)
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearches()
            _uiState.update { it.copy(recentSearches = emptyList()) }
        }
    }

    fun refresh() {
        if (_uiState.value.refreshing) return
        _uiState.update { it.copy(refreshing = true) }
        loadCategories()
        loadProducts(reset = true)
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        loadProducts(reset = true)
    }

    fun loadProducts(reset: Boolean = true) {
        val state = _uiState.value
        if (!reset && (state.loadingMore || state.currentPage + 1 >= state.totalPages)) return
        viewModelScope.launch {
            val page = if (reset) 0 else state.currentPage + 1
            if (reset) _uiState.update { it.copy(products = LoadState.Loading) } else _uiState.update { it.copy(loadingMore = true) }
            repository.products(_uiState.value.query, _uiState.value.selectedCategoryId, page).fold(
                onSuccess = { response ->
                    _uiState.update { current ->
                        val existing = (current.products as? LoadState.Success)?.data.orEmpty()
                        current.copy(
                            products = LoadState.Success(if (reset) response.content else existing + response.content),
                            currentPage = response.page,
                            totalPages = response.totalPages,
                            loadingMore = false,
                            refreshing = false,
                        )
                    }
                    loadLocalBrowsingActivity()
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(products = if (reset) LoadState.Error(error.message ?: "Không tải được sản phẩm") else current.products, loadingMore = false)
                    }
                },
            )
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    fun loadProduct(slug: String) {
        viewModelScope.launch {
            _productState.value = ProductUiState(detail = LoadState.Loading, reviews = LoadState.Idle)
            repository.product(slug).fold(
                onSuccess = { product ->
                    _productState.value = ProductUiState(detail = LoadState.Success(product), reviews = LoadState.Loading)
                    loadLocalBrowsingActivity()
                    repository.productReviews(product.id).fold(
                        { reviews -> _productState.update { it.copy(reviews = LoadState.Success(reviews.content)) } },
                        { error -> _productState.update { it.copy(reviews = LoadState.Error(error.message ?: "Không tải được đánh giá")) } },
                    )
                },
                onFailure = { error -> _productState.value = ProductUiState(detail = LoadState.Error(error.message ?: "Không tải được sản phẩm")) },
            )
        }
    }

    private fun loadLocalBrowsingActivity() {
        viewModelScope.launch {
            repository.recentSearches().onSuccess { searches ->
                _uiState.update { it.copy(recentSearches = searches) }
            }
            repository.recentlyViewed().onSuccess { products ->
                _uiState.update { it.copy(recentlyViewed = products) }
            }
        }
    }
}

package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class Scan2ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Scan2ShopRepository(db.scan2ShopDao())

    // --- Adaptive UI Dark Mode State ---
    val isDarkMode = MutableStateFlow(false)

    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // --- Default Store Storage and Prompts ---
    private val prefs = application.getSharedPreferences("Scan2ShopPrefs", Context.MODE_PRIVATE)
    val defaultStore = MutableStateFlow<String?>(prefs.getString("default_store_name", null))
    val promptSetDefaultStore = MutableStateFlow<String?>(null)

    fun setDefaultStore(store: String?) {
        defaultStore.value = store
        prefs.edit().putString("default_store_name", store).apply()
    }

    fun dismissDefaultStorePrompt() {
        promptSetDefaultStore.value = null
    }

    fun checkAndPromptDefaultStore(store: String) {
        val currentDefault = defaultStore.value
        if (currentDefault?.equals(store, ignoreCase = true) == true) return
        
        viewModelScope.launch {
            // Check if there are 2 or more products with this selectedStore in the current shopping list
            val currentList = repository.shoppingList.first()
            val count = currentList.count { it.selectedStore?.equals(store, ignoreCase = true) == true }
            if (count >= 2) {
                promptSetDefaultStore.value = store
            }
        }
    }

    // --- Search Query State ---
    val searchQuery = MutableStateFlow("")

    // Initialize Database seeding
    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // --- State Streams ---
    val shoppingList: StateFlow<List<ShoppingListItem>> = repository.shoppingList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPriceReports: StateFlow<List<PriceReport>> = repository.allPriceReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalShoppingListPrice: StateFlow<Double> = combine(
        repository.shoppingList,
        repository.allPriceReports
    ) { list, reports ->
        val reportMap = reports.groupBy { it.productId }
        list.sumOf { item ->
            val productReports = item.productId?.let { reportMap[it] } ?: emptyList()
            val finalPrice = if (item.selectedStore != null) {
                val storeReport = productReports.find { it.storeName.equals(item.selectedStore, ignoreCase = true) }
                storeReport?.price ?: (productReports.minByOrNull { it.price }?.price ?: 1.50)
            } else {
                productReports.minByOrNull { it.price }?.price ?: 1.50
            }
            finalPrice * item.quantity
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val searchResults: StateFlow<List<Product>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allProducts
            } else {
                repository.searchProducts(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Detail View Screen State ---
    private val _selectedProductId = MutableStateFlow<Int?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProductId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getProductById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeProductPriceReports: StateFlow<List<PriceReport>> = _selectedProductId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getPriceReportsForProduct(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectProduct(productId: Int?) {
        _selectedProductId.value = productId
    }

    // --- Price Contribution State (Mockup 2) ---
    val storesList = MutableStateFlow(listOf("Spar", "TM Pick n Pay", "Oceans", "Greens"))

    fun addCustomStore(storeName: String) {
        val trimmed = storeName.trim()
        if (trimmed.isNotEmpty() && !storesList.value.any { it.equals(trimmed, ignoreCase = true) }) {
            storesList.value = storesList.value + trimmed
        }
    }

    val selectedStore = MutableStateFlow("Spar")
    val enteredPrice = MutableStateFlow("0.00")
    val contributionProduct = MutableStateFlow<Product?>(null)
    val showContributionSuccess = MutableStateFlow(false)

    fun initPriceContribution(product: Product) {
        contributionProduct.value = product
        enteredPrice.value = ""
        showContributionSuccess.value = false
    }

    fun selectStoreForContribution(store: String) {
        selectedStore.value = store
    }

    fun addDigitToPrice(digit: String) {
        val current = enteredPrice.value.replace(".", "").replace(",", "")
        if (digit == "." || digit == ",") return // We handle formatting mathematically
        
        // Let's build decimal logic (e.g. keying digits adds to cents)
        val cleaned = if (current == "000") digit else current + digit
        val numeric = cleaned.toDoubleOrNull() ?: 0.0
        val priceVal = numeric / 100.0
        enteredPrice.value = String.format("%.2f", priceVal)
    }

    fun backspacePrice() {
        val current = enteredPrice.value.replace(".", "")
        if (current.length <= 1) {
            enteredPrice.value = "0.00"
            return
        }
        val shortened = current.substring(0, current.length - 1)
        val numeric = shortened.toDoubleOrNull() ?: 0.0
        val priceVal = numeric / 100.0
        enteredPrice.value = String.format("%.2f", priceVal)
    }

    fun submitPriceContribution(onComplete: () -> Unit = {}) {
        val product = contributionProduct.value ?: return
        val priceStr = enteredPrice.value
        val priceVal = priceStr.toDoubleOrNull() ?: 0.0
        if (priceVal <= 0.0) return

        val store = selectedStore.value
        val distance = if (store == "Spar") 0.5 else if (store == "TM Pick n Pay") 0.8 else 1.2

        viewModelScope.launch {
            repository.insertPriceReport(
                PriceReport(
                    productId = product.id,
                    storeName = store,
                    price = priceVal,
                    updatedTime = System.currentTimeMillis(),
                    distanceMiles = distance
                )
            )
            showContributionSuccess.value = true
            onComplete()
            // After submitting, check if this is in our shopping list to update its suggestions
            updateCheaperStoreInShoppingList(product, store, priceVal)
        }
    }

    private suspend fun updateCheaperStoreInShoppingList(product: Product, store: String, price: Double) {
        // Find if this product is in shopping list and update its productId info
        val list = repository.shoppingList.first()
        val matchingItem = list.find { it.productName.equals(product.name, ignoreCase = true) || it.barcode == product.barcode }
        if (matchingItem != null) {
            repository.insertShoppingItem(matchingItem.copy(productId = product.id))
        }
    }

    // --- Shopping List Actions (Mockup 1) ---
    fun addShoppingItem(name: String, quantity: Int = 1, unit: String = "pcs", category: String = "Produce") {
        viewModelScope.launch {
            // Check if name matches any existing catalog product
            val products = repository.allProducts.first()
            val matchedProduct = products.find { it.name.equals(name, ignoreCase = true) }
            
            repository.insertShoppingItem(
                ShoppingListItem(
                    productName = name,
                    barcode = matchedProduct?.barcode,
                    quantity = quantity,
                    unit = unit,
                    category = matchedProduct?.category ?: category,
                    productId = matchedProduct?.id
                )
            )
        }
    }

    fun toggleShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun clearCheckedItems() {
        viewModelScope.launch {
            repository.clearCheckedShoppingItems()
        }
    }

    // --- Barcode Scanner Processor ---
    fun processBarcodeScan(barcode: String, onProductFound: (Product) -> Unit, onProductNotFound: (String) -> Unit) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                // Automatically add to list or increment quantity
                val currentList = repository.shoppingList.first()
                val existingItem = currentList.find { it.barcode == barcode }
                if (existingItem != null) {
                    repository.updateShoppingItem(existingItem.copy(quantity = existingItem.quantity + 1))
                } else {
                    repository.insertShoppingItem(
                        ShoppingListItem(
                            productName = product.name,
                            barcode = product.barcode,
                            quantity = 1,
                            unit = "pcs",
                            category = product.category,
                            productId = product.id
                        )
                    )
                }
                
                // Navigate to dynamic detail page
                onProductFound(product)
            } else {
                // Product not found in database! Trigger quick manual addition or registration screen
                onProductNotFound(barcode)
            }
        }
    }

    fun getPriceReportsFlow(productId: Int): Flow<List<PriceReport>> {
        return repository.getPriceReportsForProduct(productId)
    }

    fun updateShoppingItemQuantity(item: ShoppingListItem, newQty: Int) {
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(quantity = newQty))
        }
    }

    // Let user quick-add a brand new product from a custom scanned barcode
    fun registerNewProductAndAddToList(name: String, barcode: String, category: String, initialPriceAtStore: Double, storeName: String, packageSize: String) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                barcode = barcode,
                category = category,
                description = "Scanned community product.",
                packageSize = packageSize
            )
            val productId = repository.insertProduct(product).toInt()
            
            // Add a price report for this store
            repository.insertPriceReport(
                PriceReport(
                    productId = productId,
                    storeName = storeName,
                    price = initialPriceAtStore,
                    updatedTime = System.currentTimeMillis(),
                    distanceMiles = 0.5
                )
            )
            
            // Add directly to shopping list
            repository.insertShoppingItem(
                ShoppingListItem(
                    productName = name,
                    barcode = barcode,
                    quantity = 1,
                    unit = "pcs",
                    category = category,
                    isChecked = false,
                    productId = productId,
                    selectedStore = storeName
                )
            )

            // Prompt default store if 2 or more are from this store
            checkAndPromptDefaultStore(storeName)
        }
    }

    fun addShoppingItemWithStore(name: String, quantity: Int = 1, unit: String = "pcs", category: String = "Produce", productId: Int? = null, selectedStore: String? = null) {
        viewModelScope.launch {
            val products = repository.allProducts.first()
            val matchedProduct = products.find { it.id == productId || it.name.equals(name, ignoreCase = true) }
            
            repository.insertShoppingItem(
                ShoppingListItem(
                    productName = name,
                    barcode = matchedProduct?.barcode,
                    quantity = quantity,
                    unit = unit,
                    category = matchedProduct?.category ?: category,
                    productId = matchedProduct?.id ?: productId,
                    selectedStore = selectedStore
                )
            )

            // Prompt default store if 2 or more are from this store
            if (selectedStore != null) {
                checkAndPromptDefaultStore(selectedStore)
            }
        }
    }

    fun updateProductDetails(product: Product, name: String, category: String, packageSize: String, description: String) {
        viewModelScope.launch {
            val updated = product.copy(
                name = name,
                category = category,
                packageSize = packageSize,
                description = description
            )
            repository.updateProduct(updated)
        }
    }

    fun updateStorePrice(report: PriceReport, newPrice: Double) {
        viewModelScope.launch {
            repository.updatePriceReport(report.copy(price = newPrice, updatedTime = System.currentTimeMillis()))
        }
    }

    fun refreshProductsCatalog() {
        viewModelScope.launch {
            // Re-trigger a fresh emission of search/products to update from repository
            val current = searchQuery.value
            searchQuery.value = ""
            kotlinx.coroutines.delay(10)
            searchQuery.value = current
        }
    }
}

package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class Scan2ShopRepository(private val dao: Scan2ShopDao) {

    val shoppingList: Flow<List<ShoppingListItem>> = dao.getShoppingList()
    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val allPriceReports: Flow<List<PriceReport>> = dao.getAllPriceReports()

    fun getProductById(id: Int): Flow<Product?> = dao.getProductById(id)
    fun getPriceReportsForProduct(productId: Int): Flow<List<PriceReport>> = dao.getPriceReportsForProduct(productId)
    fun searchProducts(query: String): Flow<List<Product>> = dao.searchProducts(query)

    suspend fun getProductByBarcode(barcode: String): Product? {
        return dao.getProductByBarcode(barcode)
    }

    suspend fun insertShoppingItem(item: ShoppingListItem): Long {
        return dao.insertShoppingItem(item)
    }

    suspend fun updateShoppingItem(item: ShoppingListItem) {
        dao.updateShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingListItem) {
        dao.deleteShoppingItem(item)
    }

    suspend fun clearCheckedShoppingItems() {
        dao.clearCheckedShoppingItems()
    }

    suspend fun insertProduct(product: Product): Long {
        return dao.insertProduct(product)
    }

    suspend fun insertPriceReport(report: PriceReport): Long {
        return dao.insertPriceReport(report)
    }

    suspend fun updateProduct(product: Product) {
        dao.updateProduct(product)
    }

    suspend fun updatePriceReport(report: PriceReport) {
        dao.updatePriceReport(report)
    }

    // Pre-populates the database with beautiful, realistic grocery items and crowd-sourced prices
    suspend fun prepopulateIfEmpty() {
        val currentProducts = allProducts.first()
        if (currentProducts.isEmpty()) {
            // 1. Seed Products
            val milk = Product(
                name = "Dairibord Fresh Milk",
                barcode = "0111100223",
                category = "Dairy & Eggs",
                description = "Nutritious and creamy Zimbabwean fresh milk pasteurised to perfection.",
                packageSize = "2 Litres"
            )
            val avocado = Product(
                name = "Fresh Hass Avocados",
                barcode = "0333831201",
                category = "Produce",
                description = "Plump, fresh, and ready-to-eat creamy locally grown Hass avocados.",
                packageSize = "4 Pack"
            )
            val honeycrisp = Product(
                name = "Royal Gala Apples",
                barcode = "0444928127",
                category = "Produce",
                description = "Sweet, crispy, and exceptionally juicy hand-picked royal gala apples.",
                packageSize = "1.5 Kg"
            )
            val yogurt = Product(
                name = "Kefalos Greek Yogurt",
                barcode = "0222238471",
                category = "Dairy & Eggs",
                description = "Thick, high-protein strained Greek style yogurt produced in Zimbabwe.",
                packageSize = "500 ml"
            )
            val bananas = Product(
                name = "Sunripe Sweet Bananas",
                barcode = "0555123456",
                category = "Produce",
                description = "Direct-trade locally grown organic sweet bananas rich in vitamin B6.",
                packageSize = "1 Kg"
            )
            val bread = Product(
                name = "Lobels Sliced White Bread",
                barcode = "0666123456",
                category = "Bakery & Bread",
                description = "Loved family recipe high-quality sliced white bread baked fresh daily.",
                packageSize = "700 g"
            )

            val milkId = dao.insertProduct(milk).toInt()
            val avocadoId = dao.insertProduct(avocado).toInt()
            val honeycrispId = dao.insertProduct(honeycrisp).toInt()
            val yogurtId = dao.insertProduct(yogurt).toInt()
            val bananasId = dao.insertProduct(bananas).toInt()
            val breadId = dao.insertProduct(bread).toInt()

            // 2. Seed Price Reports for Milk (Spar, TM Pick n Pay, Oceans, Greens)
            dao.insertPriceReports(listOf(
                PriceReport(productId = milkId, storeName = "Greens", price = 2.49, updatedTime = System.currentTimeMillis() - 15 * 60 * 1000, distanceMiles = 1.1),
                PriceReport(productId = milkId, storeName = "TM Pick n Pay", price = 2.89, updatedTime = System.currentTimeMillis() - 2 * 60 * 60 * 1000, distanceMiles = 0.8),
                PriceReport(productId = milkId, storeName = "Spar", price = 3.25, updatedTime = System.currentTimeMillis() - 5 * 60 * 60 * 1000, distanceMiles = 0.5),
                PriceReport(productId = milkId, storeName = "Oceans", price = 2.75, updatedTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000, distanceMiles = 1.5)
            ))

            // Seed Price Reports for Avocados
            dao.insertPriceReports(listOf(
                PriceReport(productId = avocadoId, storeName = "Oceans", price = 1.25, updatedTime = System.currentTimeMillis() - 2 * 60 * 60 * 1000, distanceMiles = 1.5),
                PriceReport(productId = avocadoId, storeName = "TM Pick n Pay", price = 1.49, updatedTime = System.currentTimeMillis() - 3 * 60 * 60 * 1000, distanceMiles = 0.8),
                PriceReport(productId = avocadoId, storeName = "Spar", price = 1.99, updatedTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000, distanceMiles = 0.5)
            ))

            // Seed Price Reports for Honeycrisp Apples
            dao.insertPriceReports(listOf(
                PriceReport(productId = honeycrispId, storeName = "Greens", price = 2.19, updatedTime = System.currentTimeMillis() - 4 * 60 * 60 * 1000, distanceMiles = 1.1),
                PriceReport(productId = honeycrispId, storeName = "Oceans", price = 2.39, updatedTime = System.currentTimeMillis() - 6 * 60 * 60 * 1000, distanceMiles = 1.5),
                PriceReport(productId = honeycrispId, storeName = "Spar", price = 2.99, updatedTime = System.currentTimeMillis() - 10 * 60 * 60 * 1000, distanceMiles = 0.5)
            ))

            // Seed Price Reports for Greek Yogurt
            dao.insertPriceReports(listOf(
                PriceReport(productId = yogurtId, storeName = "Spar", price = 3.99, updatedTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000, distanceMiles = 0.5),
                PriceReport(productId = yogurtId, storeName = "TM Pick n Pay", price = 4.29, updatedTime = System.currentTimeMillis() - 4 * 60 * 60 * 1000, distanceMiles = 0.8)
            ))

            // Seed Price Reports for Bananas
            dao.insertPriceReports(listOf(
                PriceReport(productId = bananasId, storeName = "TM Pick n Pay", price = 0.99, updatedTime = System.currentTimeMillis() - 2 * 60 * 60 * 1000, distanceMiles = 0.8),
                PriceReport(productId = bananasId, storeName = "Greens", price = 1.19, updatedTime = System.currentTimeMillis() - 5 * 60 * 60 * 1000, distanceMiles = 1.1)
            ))

            // Seed Price Reports for Sourdough Bread
            dao.insertPriceReports(listOf(
                PriceReport(productId = breadId, storeName = "Greens", price = 1.05, updatedTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000, distanceMiles = 1.1),
                PriceReport(productId = breadId, storeName = "Spar", price = 1.20, updatedTime = System.currentTimeMillis() - 8 * 60 * 60 * 1000, distanceMiles = 0.5)
            ))

            // 3. Seed Initial Shopping List Items so users see Mockup 1 instantly
            dao.insertShoppingItem(ShoppingListItem(
                productName = "Fresh Hass Avocados",
                barcode = "0333831201",
                quantity = 3,
                unit = "packs",
                category = "Produce",
                isChecked = true,
                productId = avocadoId
            ))
            dao.insertShoppingItem(ShoppingListItem(
                productName = "Royal Gala Apples",
                barcode = "0444928127",
                quantity = 4,
                unit = "packs",
                category = "Produce",
                isChecked = false,
                productId = honeycrispId
            ))
            dao.insertShoppingItem(ShoppingListItem(
                productName = "Dairibord Fresh Milk",
                barcode = "0111100223",
                quantity = 1,
                unit = "bottles",
                category = "Dairy & Eggs",
                isChecked = false,
                productId = milkId
            ))
            dao.insertShoppingItem(ShoppingListItem(
                productName = "Kefalos Greek Yogurt",
                barcode = "0222238471",
                quantity = 2,
                unit = "tub",
                category = "Dairy & Eggs",
                isChecked = false,
                productId = yogurtId
            ))
        }
    }
}

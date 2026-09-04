package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface Scan2ShopDao {

    // --- Shopping List Queries ---
    @Query("SELECT * FROM shopping_list_items ORDER BY isChecked ASC, id DESC")
    fun getShoppingList(): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingListItem): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingListItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingListItem)

    @Query("DELETE FROM shopping_list_items WHERE isChecked = 1")
    suspend fun clearCheckedShoppingItems()

    @Query("DELETE FROM shopping_list_items")
    suspend fun clearAllShoppingItems()


    // --- Product Catalog Queries ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Int): Flow<Product?>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)


    // --- Price Reports Queries ---
    @Query("SELECT * FROM price_reports WHERE productId = :productId ORDER BY price ASC")
    fun getPriceReportsForProduct(productId: Int): Flow<List<PriceReport>>

    @Query("SELECT * FROM price_reports ORDER BY updatedTime DESC")
    fun getAllPriceReports(): Flow<List<PriceReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceReport(report: PriceReport): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceReports(reports: List<PriceReport>)

    @Update
    suspend fun updatePriceReport(report: PriceReport)
}

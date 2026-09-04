package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val barcode: String?,
    val category: String,
    val description: String,
    val imageUrl: String = "",
    val packageSize: String = "" // Added for pack size/volume like "litres", "ml", "kg", "mm", etc.
) : Serializable

@Entity(tableName = "price_reports")
data class PriceReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val storeName: String,
    val price: Double,
    val updatedTime: Long,
    val distanceMiles: Double,
    val reporterEmail: String = "community_hero@scan2shop.com"
) : Serializable

@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productName: String,
    val barcode: String?,
    val quantity: Int = 1,
    val unit: String = "pcs",
    val category: String = "Other",
    val isChecked: Boolean = false,
    val productId: Int? = null,
    val selectedStore: String? = null
) : Serializable

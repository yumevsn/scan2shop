@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.data.*
import com.example.ui.Scan2ShopViewModel

// ==========================================
// 1. SHOPPING LIST SCREEN (Mockup 1)
// ==========================================
@Composable
fun ShoppingListScreen(
    viewModel: Scan2ShopViewModel,
    onOpenProductDetails: (Product) -> Unit,
    onOpenScanner: () -> Unit
) {
    val shoppingList by viewModel.shoppingList.collectAsState()
    val allPriceReports by viewModel.allPriceReports.collectAsState(initial = emptyList())
    val allProducts by viewModel.allProducts.collectAsState(initial = emptyList())
    val totalPrice by viewModel.totalShoppingListPrice.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shopping_list_screen")
    ) {
        // Prominent SCAN CARD (No manual text entry row)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onOpenScanner() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Barcode to Add",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SCAN BARCODE TO ADD TO LIST",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Sub-bar to clear checked or quick stats
        if (shoppingList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val checkedCount = shoppingList.count { it.isChecked }
                Text(
                    text = "$checkedCount of ${shoppingList.size} items checked",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (shoppingList.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your list is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Products must be added by scanning to help log community prices accurately. Scan items to find cheap Zimbabwe prices!",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Group lists by category
            val grouped = shoppingList.groupBy { it.category }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                grouped.forEach { (category, items) ->
                    item {
                        CategoryHeader(name = category, count = items.size)
                    }
                    items(items) { item ->
                        val productReports = item.productId?.let { id -> allPriceReports.filter { it.productId == id } } ?: emptyList()
                        val selectedStoreReport = if (item.selectedStore != null) {
                            productReports.find { it.storeName.equals(item.selectedStore, ignoreCase = true) }
                        } else null

                        val activePrice = selectedStoreReport?.price ?: (productReports.minByOrNull { it.price }?.price ?: 1.50)
                        val activeReportName = item.selectedStore ?: (productReports.minByOrNull { it.price }?.storeName ?: "Any Store")

                        val matchingProduct = allProducts.find { it.id == item.productId }
                        val packageSize = matchingProduct?.packageSize ?: ""

                        ShoppingItemRow(
                            item = item,
                            cheapestPrice = activePrice,
                            cheapestStoreName = activeReportName,
                            packageSize = packageSize,
                            onToggleChecked = { viewModel.toggleShoppingItem(item) },
                            onIncrement = { viewModel.updateShoppingItemQuantity(item, item.quantity + 1) },
                            onDecrement = {
                                if (item.quantity > 1) {
                                    viewModel.updateShoppingItemQuantity(item, item.quantity - 1)
                                } else {
                                    viewModel.deleteShoppingItem(item)
                                }
                            },
                            onDelete = { viewModel.deleteShoppingItem(item) },
                            onViewCheaperStore = {
                                if (item.productId != null) {
                                    viewModel.selectProduct(item.productId)
                                    viewModel.allProducts.value.find { it.id == item.productId }?.let {
                                        onOpenProductDetails(it)
                                    }
                                }
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // STICKY bottom total row
        if (shoppingList.isNotEmpty()) {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED TOTAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format("$%.2f", totalPrice),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearCheckedItems() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("clear_checked_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Clear Checked", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryColor(name: String): Color {
    return when (name.lowercase().trim()) {
        "produce" -> Color(0xFF10B981) // Emerald
        "dairy", "dairy & eggs" -> Color(0xFF3B82F6) // Blue
        "bakery", "bakery & bread" -> Color(0xFFF59E0B) // Amber
        "meat", "butchery & poultry" -> Color(0xFFEF4444) // Red
        "fish & seafood" -> Color(0xFF06B6D4) // Cyan
        "pantry staples" -> Color(0xFF84CC16) // Lime
        "grains & pasta" -> Color(0xFFF97316) // Orange
        "canned goods" -> Color(0xFFD97706) // Dark Orange
        "beverages" -> Color(0xFF0EA5E9) // Sky Blue
        "snacks & sweets" -> Color(0xFFEC4899) // Pink
        "frozen foods" -> Color(0xFF38BDF8) // Light Sky Blue
        "deli & prepared foods" -> Color(0xFFE11D48) // Rose
        "household & cleaning", "cleaning & laundry", "household supplies" -> Color(0xFF8B5CF6) // Violet
        "hardware & tools" -> Color(0xFF78716C) // Stone/Grey
        "electronics & tech" -> Color(0xFF3B82F6) // Bright blue
        "home & living" -> Color(0xFF14B8A6) // Teal
        "personal care" -> Color(0xFFA78BFA) // Light Violet
        "baby products" -> Color(0xFFF472B6) // Light Pink
        "pet supplies" -> Color(0xFF14B8A6) // Teal
        "pharmacy & health" -> Color(0xFF10B981) // Mint
        "garden & outdoor" -> Color(0xFF22C55E) // Green
        "office & stationery" -> Color(0xFF64748B) // Slate
        "other" -> Color(0xFF6B7280) // Gray
        else -> Color(0xFF6B7280) // Gray
    }
}

@Composable
fun CategoryHeader(name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Color bar indicator based on Category
            Spacer(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getCategoryColor(name))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$count ${if (count == 1) "ITEM" else "ITEMS"}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingListItem,
    cheapestPrice: Double,
    cheapestStoreName: String,
    packageSize: String,
    onToggleChecked: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
    onViewCheaperStore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (item.isChecked) MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Tier 1: Checkbox, Product Name/Metadata, Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                IconButton(
                    onClick = onToggleChecked,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (item.isChecked) "Checked" else "Unchecked",
                        tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Product Name (now has maximum horizontal space to fit beautifully!)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (packageSize.isNotEmpty()) "${item.productName} ($packageSize)" else item.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tier 2: Quantity Controls, Cheapest Store Pill, and Row Total Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity Selector Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove, 
                            contentDescription = "Decrease Quantity", 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Increase Quantity", 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Community Store Pill
                if (item.productId != null && cheapestStoreName.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .clickable { onViewCheaperStore() }
                            .padding(horizontal = 4.dp),
                        color = Color(0xFFDBEAFE), // Elegant blue pill
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color(0xFF1E40AF),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("$%.2f at %s", cheapestPrice, cheapestStoreName),
                                color = Color(0xFF1E40AF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Row Total
                Text(
                    text = String.format("$%.2f", cheapestPrice * item.quantity),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (item.isChecked) Color.Gray.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}


// ==========================================
// 2. PRODUCTS BROWSE SCREEN (Search / Catalog Tab)
// ==========================================
@Composable
fun ProductsBrowseScreen(
    viewModel: Scan2ShopViewModel,
    onOpenProductDetails: (Product) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var productToAdd by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catalog_screen")
    ) {
        // Search Input Column
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search community product catalog...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Banner statistics
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "A community directory of crowdsourced pricing compiled by 4,320 local grocery shoppers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        var isRefreshing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.refreshProductsCatalog()
                    kotlinx.coroutines.delay(1200)
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No products found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Try a different search, or use scanner to add missing ones.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectProduct(product.id)
                                    onOpenProductDetails(product)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product Illustration Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getProductEmoji(product.name),
                                        fontSize = 28.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = product.category.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "UPC: ${product.barcode ?: "Manual"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { productToAdd = product },
                                        modifier = Modifier.testTag("add_to_list_direct_${product.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.AddShoppingCart,
                                            contentDescription = "Add to list directly",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "View detail",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (productToAdd != null) {
        val pr = productToAdd!!
        var quantity by remember { mutableStateOf(1) }
        val storesList = listOf("Spar", "TM Pick n Pay", "OK Zimbabwe", "Food Lovers Market", "Choppies", "Oceans Supermarket", "Greens Supermarket")
        val defaultStoreVal by viewModel.defaultStore.collectAsState()
        var selectedStore by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(productToAdd, defaultStoreVal) {
            if (productToAdd != null) {
                selectedStore = defaultStoreVal
            }
        }

        AlertDialog(
            onDismissRequest = { productToAdd = null },
            title = { Text("Add to Shopping List", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = pr.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (pr.packageSize.isNotEmpty()) {
                        Text("Pack Size: ${pr.packageSize}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Quantity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$quantity",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Select Shopping Store (Optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        storesList.forEach { store ->
                            val isSelected = selectedStore == store
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedStore = if (isSelected) null else store
                                },
                                label = { Text(store, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addShoppingItemWithStore(
                            name = pr.name,
                            quantity = quantity,
                            unit = "pcs",
                            category = pr.category,
                            productId = pr.id,
                            selectedStore = selectedStore
                        )
                        productToAdd = null
                    }
                ) {
                    Text("Add to List", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToAdd = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ==========================================
// 3. PRODUCT DETAILS SCREEN (Mockup 3)
// ==========================================
@Composable
fun ProductDetailsScreen(
    product: Product,
    viewModel: Scan2ShopViewModel,
    onOpenContribution: (Product) -> Unit,
    onClose: () -> Unit
) {
    val reports by viewModel.activeProductPriceReports.collectAsState()
    val context = LocalContext.current
    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var priceReportToEdit by remember { mutableStateOf<PriceReport?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .testTag("product_detail_screen")
    ) {
        // High fidelity Top Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Row {
                    IconButton(onClick = { showEditDetailsDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Product Details", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Share Product: ${product.name}")
                            val barcodeOrId = if (product.barcode.isNullOrEmpty()) product.id.toString() else product.barcode
                            val link = "https://ais-pre-dhxj6a7sggg6j74goy7wjl-861686086527.europe-west2.run.app/product/$barcodeOrId"
                            putExtra(Intent.EXTRA_TEXT, "Check out ${product.name} on Scan2Shop! Compare prices across stores here: $link")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Product"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Custom Product Illustration (No generic offline-failing URLs!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = getProductEmoji(product.name),
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan2Shop Community Verified",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Product Information Text block
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Category Tag badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = product.category.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }

            // Title
            Text(
                text = product.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // UPC label
            Text(
                text = "Barcode: ${product.barcode ?: "None (Manual Entry)"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = product.description,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nearby Availability block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Nearby Availability",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${reports.size} STORES FOUND",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // CHEAPEST to EXPENSIVE visual slide bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CHEAPEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Text("EXPENSIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF059669), // Green cheapest
                                Color(0xFFFBBF24), // Yellow average
                                Color(0xFFEF4444)  // Red premium
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Store Prices rows list
            if (reports.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No prices reported yet.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Be the first contributor to log a price for this product!", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reports.forEachIndexed { index, report ->
                        StorePriceRow(
                            report = report,
                            isCheapest = index == 0,
                            isMostExpensive = index == reports.size - 1 && reports.size > 1,
                            onEditPrice = { priceReportToEdit = report }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ADD A NEW PRICE BUTTON
            Button(
                onClick = { onOpenContribution(product) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_price_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add a New Price", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showEditDetailsDialog) {
        val categories = listOf(
            "Produce", 
            "Dairy & Eggs", 
            "Bakery & Bread", 
            "Butchery & Poultry", 
            "Fish & Seafood",
            "Pantry Staples", 
            "Grains & Pasta",
            "Canned Goods",
            "Beverages",
            "Snacks & Sweets",
            "Frozen Foods",
            "Deli & Prepared Foods",
            "Household & Cleaning",
            "Cleaning & Laundry",
            "Hardware & Tools",
            "Electronics & Tech",
            "Home & Living",
            "Personal Care",
            "Baby Products",
            "Pet Supplies",
            "Pharmacy & Health",
            "Garden & Outdoor",
            "Office & Stationery",
            "Other"
        )
        val units = listOf("Litres", "ml", "Kg", "g", "mm", "pcs", "packs", "bottles", "tubs")

        var editedName by remember { mutableStateOf(product.name) }
        var editedCategory by remember { mutableStateOf(product.category) }
        var sizeAmount by remember {
            mutableStateOf(
                if (product.packageSize.isEmpty()) ""
                else {
                    val trimmed = product.packageSize.trim()
                    val matched = units.find { trimmed.endsWith(it, ignoreCase = true) }
                    if (matched != null) {
                        trimmed.substring(0, trimmed.length - matched.length).trim()
                    } else {
                        val lastSpace = trimmed.lastIndexOf(' ')
                        if (lastSpace != -1) trimmed.substring(0, lastSpace).trim() else trimmed
                    }
                }
            )
        }
        var sizeUnit by remember {
            mutableStateOf(
                if (product.packageSize.isEmpty()) "pcs"
                else {
                    val trimmed = product.packageSize.trim()
                    val matched = units.find { trimmed.endsWith(it, ignoreCase = true) }
                    matched ?: "pcs"
                }
            )
        }
        var editedDescription by remember { mutableStateOf(product.description) }

        var expandedCat by remember { mutableStateOf(false) }
        var expandedUnit by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDetailsDialog = false },
            title = { Text("Edit Product Details", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(editedCategory))
                                    )
                                }
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.ArrowDropDown, 
                                    contentDescription = "Dropdown",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandedCat = true }
                        )
                        DropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            categories.forEachIndexed { index, cat ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                    )
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(cat))
                                        )
                                    },
                                    text = { 
                                        Text(
                                            cat, 
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        editedCategory = cat
                                        expandedCat = false
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Pack Size Row
                    Text("Pack Size", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = sizeAmount,
                            onValueChange = { sizeAmount = it },
                            placeholder = { Text("e.g. 2, 500, 1.5") },
                            label = { Text("Amount") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = sizeUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.ArrowDropDown, 
                                        contentDescription = "DropdownUnit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expandedUnit = true }
                            )
                            DropdownMenu(
                                expanded = expandedUnit,
                                onDismissRequest = { expandedUnit = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                units.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                unit, 
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ) 
                                        },
                                        onClick = {
                                            sizeUnit = unit
                                            expandedUnit = false
                                        },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text("Description") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalPackageSize = if (sizeAmount.isNotBlank()) "${sizeAmount.trim()} $sizeUnit" else ""
                        viewModel.updateProductDetails(product, editedName, editedCategory, finalPackageSize, editedDescription)
                        showEditDetailsDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDetailsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (priceReportToEdit != null) {
        val rep = priceReportToEdit!!
        var editedPriceStr by remember { mutableStateOf(rep.price.toString()) }

        AlertDialog(
            onDismissRequest = { priceReportToEdit = null },
            title = { Text("Edit Store Price", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text("Set new price for this product at ${rep.storeName}:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedPriceStr,
                        onValueChange = { editedPriceStr = it },
                        label = { Text("Price ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = editedPriceStr.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            viewModel.updateStorePrice(rep, parsed)
                            priceReportToEdit = null
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { priceReportToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StorePriceRow(
    report: PriceReport,
    isCheapest: Boolean,
    isMostExpensive: Boolean,
    onEditPrice: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isCheapest) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = report.storeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEditPrice,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit store price",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Updated ${getRecencyText(report.updatedTime)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Price label pill
                if (isCheapest) {
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "BEST DEAL",
                            color = Color(0xFF065F46),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isMostExpensive) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "PREMIUM",
                            color = Color(0xFF991B1B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "AVERAGE",
                            color = Color(0xFF374151),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = String.format("$%.2f", report.price),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isCheapest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


// ==========================================
// 4. PRICE CONTRIBUTION FORM (Mockup 2)
// ==========================================
@Composable
fun PriceContributionScreen(
    product: Product,
    viewModel: Scan2ShopViewModel,
    onBack: () -> Unit
) {
    val selectedStore by viewModel.selectedStore.collectAsState()
    val enteredPrice by viewModel.enteredPrice.collectAsState()
    val showSuccess by viewModel.showContributionSuccess.collectAsState()

    var showAddStoreDialog by remember { mutableStateOf(false) }
    var newStoreFieldText by remember { mutableStateOf("") }

    // Dialog for adding custom store
    if (showAddStoreDialog) {
        AlertDialog(
            onDismissRequest = { showAddStoreDialog = false },
            title = { Text("Add Custom Store", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the name of the new supermarket or retail store:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newStoreFieldText,
                        onValueChange = { newStoreFieldText = it },
                        label = { Text("Store Name (e.g. Oceans, Greens)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameClean = newStoreFieldText.trim()
                        if (nameClean.isNotEmpty()) {
                            viewModel.addCustomStore(nameClean)
                            viewModel.selectStoreForContribution(nameClean)
                        }
                        newStoreFieldText = ""
                        showAddStoreDialog = false
                    }
                ) {
                    Text("Add Store", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("contribution_screen")
    ) {
        // Simple Top App Bar
        TopAppBar(
            title = { Text("Log Store Price", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (showSuccess) {
            // "Thanks for helping" custom card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.VolunteerActivism, // Hand with heart equivalent outline
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Thank You!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Thanks for helping the community save! Your crowdsourced price was dynamically logged under the product file.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.showContributionSuccess.value = false
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            val canSubmit = enteredPrice != "0.00" && enteredPrice.isNotEmpty()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Store Location Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Store Location",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = { showAddStoreDialog = true },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                Icons.Default.AddCircleOutline, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Add Store", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("NEARBY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Horizontal Store suggestions selector
                val dynamicStores by viewModel.storesList.collectAsState()
                val storesList = dynamicStores.mapIndexed { idx, storeName ->
                    val dist = if (storeName == "Spar") "0.5 miles"
                               else if (storeName == "TM Pick n Pay") "0.8 miles"
                               else if (storeName == "Oceans") "1.5 miles"
                               else if (storeName == "Greens") "1.1 miles"
                               else String.format("%.1f mi", 1.0 + (idx * 0.3) % 4.0)
                    ContributionStore(storeName, dist, idx == 0)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    storesList.forEach { store ->
                        val isSelected = selectedStore == store.name
                        Card(
                            onClick = { viewModel.selectStoreForContribution(store.name) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.LightGray.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.width(132.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = if (isSelected) "SELECTED" else "SUGGESTED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = store.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = store.distance,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Section 2: Enter Price block
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ENTER PRICE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Flashing display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 3.dp)
                            )
                            Text(
                                text = enteredPrice.ifEmpty { "0.00" },
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            // Animated vertical cursor bar
                            Text(
                                text = "|",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Product Title Info
                        Text(
                            text = product.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Section 3: High Fidelity custom Numpad Grid
                CustomNumpadGrid(
                    onDigit = { viewModel.addDigitToPrice(it) },
                    onBackspace = { viewModel.backspacePrice() }
                )
            }

            // Sticky Bottom Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.submitPriceContribution() },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_price_btn")
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.VolunteerActivism,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Thanks for helping the community save!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

data class ContributionStore(val name: String, val distance: String, val isNearby: Boolean)

// Custom visual keypad for grocery price loggers
@Composable
fun CustomNumpadGrid(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                if (char == "DEL") onBackspace()
                                else onDigit(char)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (char == "DEL") {
                            Icon(
                                Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = char,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// ==========================================
// 5. REGISTER NEW COMMUNITY PRODUCT (Missing Barcode Form)
// ==========================================
@Composable
fun RegisterNewProductScreen(
    barcode: String,
    viewModel: Scan2ShopViewModel,
    onRegistrationSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var prName by remember { mutableStateOf("") }
    var prCategory by remember { mutableStateOf("Produce") }
    var prPrice by remember { mutableStateOf("") }

    // Dynamic stores retrieved from ViewModel Flow!
    val stores by viewModel.storesList.collectAsState()
    val defaultStoreVal by viewModel.defaultStore.collectAsState()
    var prStore by remember { mutableStateOf("") }
    
    // Set default store when list is available or updated
    LaunchedEffect(stores, defaultStoreVal) {
        if (prStore.isEmpty() && stores.isNotEmpty()) {
            prStore = defaultStoreVal ?: stores.first()
        }
    }

    var showAddStoreDialog by remember { mutableStateOf(false) }
    var newStoreFieldText by remember { mutableStateOf("") }

    // Pack Size fields (Allows Litre, ml, kg, g, mm, etc.)
    var sizeAmount by remember { mutableStateOf("") }
    var sizeUnit by remember { mutableStateOf("Litres") }
    var expandedUnit by remember { mutableStateOf(false) }
    val units = listOf("Litres", "ml", "Kg", "g", "mm", "pcs", "packs", "bottles", "tubs")

    val categories = listOf(
        "Produce", 
        "Dairy & Eggs", 
        "Bakery & Bread", 
        "Butchery & Poultry", 
        "Fish & Seafood",
        "Pantry Staples", 
        "Grains & Pasta",
        "Canned Goods",
        "Beverages",
        "Snacks & Sweets",
        "Frozen Foods",
        "Deli & Prepared Foods",
        "Household & Cleaning",
        "Cleaning & Laundry",
        "Hardware & Tools",
        "Electronics & Tech",
        "Home & Living",
        "Personal Care",
        "Baby Products",
        "Pet Supplies",
        "Pharmacy & Health",
        "Garden & Outdoor",
        "Office & Stationery",
        "Other"
    )

    var expandedCat by remember { mutableStateOf(false) }
    var expandedStore by remember { mutableStateOf(false) }

    // Dialog for adding custom store
    if (showAddStoreDialog) {
        AlertDialog(
            onDismissRequest = { showAddStoreDialog = false },
            title = { Text("Add Custom Store", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the name of the new supermarket or retail store:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newStoreFieldText,
                        onValueChange = { newStoreFieldText = it },
                        label = { Text("Store Name (e.g. Oceans, Greens)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameClean = newStoreFieldText.trim()
                        if (nameClean.isNotEmpty()) {
                            viewModel.addCustomStore(nameClean)
                            prStore = nameClean
                        }
                        newStoreFieldText = ""
                        showAddStoreDialog = false
                    }
                ) {
                    Text("Add Store", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("missing_register_screen")
    ) {
        TopAppBar(
            title = { Text("Unrecognized Barcode", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Explanatory card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Be a Savings Pioneer!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("This barcode ($barcode) is not in our catalog yet. Add it to help other shoppers!", fontSize = 12.sp)
                    }
                }
            }

            Text("Product Registration", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

            // Field: Barcode (Readonly)
            OutlinedTextField(
                value = barcode,
                onValueChange = {},
                readOnly = true,
                label = { Text("Scanned Barcode ID") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // Field: Product Name
            OutlinedTextField(
                value = prName,
                onValueChange = { prName = it },
                label = { Text("Product Name (e.g. Cremora Creamy Milk)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("register_name_input"),
                shape = RoundedCornerShape(8.dp)
            )

            // Field: Category Selector
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                OutlinedTextField(
                    value = prCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(getCategoryColor(prCategory))
                            )
                        }
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.ArrowDropDown, 
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                // Invisible overlay to capture clicks anywhere on the field
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expandedCat = true }
                )
                DropdownMenu(
                    expanded = expandedCat,
                    onDismissRequest = { expandedCat = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                ) {
                    categories.forEachIndexed { index, cat ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            )
                        }
                        DropdownMenuItem(
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(cat))
                                )
                            },
                            text = { 
                                Text(
                                    cat, 
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            onClick = {
                                prCategory = cat
                                expandedCat = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.primary,
                                trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // FIELD: Package Quantity / Volume with Unit (litres, ml, kg, mm etc.)
            Text(
                "Package Size & Quantity Unit", 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp, 
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = sizeAmount,
                    onValueChange = { sizeAmount = it },
                    placeholder = { Text("e.g. 2, 500, 1.5") },
                    label = { Text("Measurement Amount") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = sizeUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown, 
                                contentDescription = "DropdownUnit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedUnit = true }
                    )
                    DropdownMenu(
                        expanded = expandedUnit,
                        onDismissRequest = { expandedUnit = false },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        unit, 
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ) 
                                },
                                onClick = {
                                    sizeUnit = unit
                                    expandedUnit = false
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }
            }

            // Field: Price in store
            OutlinedTextField(
                value = prPrice,
                onValueChange = { prPrice = it },
                label = { Text("Current Price (e.g., 2.49)") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("register_price_input"),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = { Text("$", fontWeight = FontWeight.Bold) }
            )

            // Field: Store Retailer with Custom Store Addition button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Retailer Store Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = { showAddStoreDialog = true },
                    modifier = Modifier.height(32.dp).padding(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add custom store", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                OutlinedTextField(
                    value = prStore,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Store") },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.ArrowDropDown, 
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                // Invisible overlay to capture clicks anywhere on the field
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expandedStore = true }
                )
                DropdownMenu(
                    expanded = expandedStore,
                    onDismissRequest = { expandedStore = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                ) {
                    stores.forEach { store ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    store, 
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            onClick = {
                                prStore = store
                                expandedStore = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.primary,
                                trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        }

        // Sticky Action Buttons Row at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }

                val formValid = prName.isNotBlank() && prPrice.toDoubleOrNull() != null && prPrice.toDouble() > 0 && prStore.isNotBlank()
                Button(
                    onClick = {
                        val priceNum = prPrice.toDoubleOrNull() ?: 1.0
                        val finalPackageSize = if (sizeAmount.isNotBlank()) "${sizeAmount.trim()} $sizeUnit" else ""
                        viewModel.registerNewProductAndAddToList(
                            name = prName,
                            barcode = barcode,
                            category = prCategory,
                            initialPriceAtStore = priceNum,
                            storeName = prStore,
                            packageSize = finalPackageSize
                        )
                        onRegistrationSuccess()
                    },
                    enabled = formValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(1.5f).height(50.dp).testTag("register_save_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Register & Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}


// ==========================================
// UTILS & GRAPHICS HELPERS
// ==========================================
private fun getProductEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.contains("milk") -> "🥛"
        lower.contains("avocado") -> "🥑"
        lower.contains("apple") -> "🍎"
        lower.contains("banana") -> "🍌"
        lower.contains("yogurt") -> "🥣"
        lower.contains("bread") || lower.contains("baguette") -> "🥖"
        lower.contains("egg") -> "🥚"
        lower.contains("cheese") -> "🧀"
        lower.contains("strawberry") || lower.contains("berries") -> "🍓"
        else -> "🛒"
    }
}

private fun getRecencyText(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (1000 * 60)
    val hours = mins / 60
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${hours / 24}d ago"
    }
}


// ==========================================
// 8. SETTINGS & ABOUT SCREEN
// ==========================================
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Text(
            text = "Scan2Shop Settings",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )

        // About the App Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "About This App",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "Scan2Shop makes it easy to do shopping, compare crowdsourced prices across supermarkets, and shop easier! By scanning product barcodes or uploading receipts, you can unlock community reports and always find where the cheapest essential goods are currently sold in Zimbabwe.",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Share & Install Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Share & Install",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Let others scan this custom QR code on your screen to instantly install and start using Scan2Shop, or share a direct link!",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // The Custom drawn QR Code!
                MockQRCode()

                Button(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Install Scan2Shop to make shopping easy and compare supermarket prices! Compare live community prices here: https://ais-pre-dhxj6a7sggg6j74goy7wjl-861686086527.europe-west2.run.app/"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Scan2Shop Link")
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Direct Link", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun MockQRCode(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(160.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val blockSize = size.width / 15f
            val qrColor = Color(0xFF1E293B) // slate-900
            
            // Top-Left Finder block
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(blockSize * 5, blockSize * 5),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(blockSize, blockSize),
                size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(blockSize * 1.5f, blockSize * 1.5f),
                size = androidx.compose.ui.geometry.Size(blockSize * 2, blockSize * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
            )

            // Top-Right Finder block
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(size.width - blockSize * 5, 0f),
                size = androidx.compose.ui.geometry.Size(blockSize * 5, blockSize * 5),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width - blockSize * 4, blockSize),
                size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(size.width - blockSize * 3.5f, blockSize * 1.5f),
                size = androidx.compose.ui.geometry.Size(blockSize * 2, blockSize * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
            )

            // Bottom-Left Finder block
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(0f, size.height - blockSize * 5),
                size = androidx.compose.ui.geometry.Size(blockSize * 5, blockSize * 5),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(blockSize, size.height - blockSize * 4),
                size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(blockSize * 1.5f, size.height - blockSize * 3.5f),
                size = androidx.compose.ui.geometry.Size(blockSize * 2, blockSize * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
            )

            // Random Dots
            val randomDots = listOf(
                Pair(6, 1), Pair(7, 2), Pair(8, 0), Pair(9, 3), Pair(8, 4),
                Pair(1, 6), Pair(3, 7), Pair(4, 8), Pair(0, 9), Pair(2, 8),
                Pair(6, 6), Pair(7, 7), Pair(8, 8), Pair(9, 9), Pair(10, 10),
                Pair(11, 11), Pair(12, 12), Pair(13, 13), Pair(14, 14),
                Pair(11, 2), Pair(12, 3), Pair(13, 1), Pair(14, 2), Pair(10, 3),
                Pair(2, 11), Pair(3, 12), Pair(1, 13), Pair(2, 14), Pair(3, 10),
                Pair(6, 11), Pair(7, 12), Pair(8, 13), Pair(9, 14), Pair(10, 12),
                Pair(11, 6), Pair(12, 7), Pair(13, 8), Pair(14, 9), Pair(12, 10)
            )
            randomDots.forEach { (x, y) ->
                drawRect(
                    color = qrColor,
                    topLeft = Offset(x * blockSize, y * blockSize),
                    size = androidx.compose.ui.geometry.Size(blockSize, blockSize)
                )
            }
        }
    }
}

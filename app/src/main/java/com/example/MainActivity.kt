@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Product
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.Scan2ShopViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

enum class TabMode {
    PRODUCTS, LIST, SETTINGS
}

enum class ActiveScreen {
    NONE, SCANNER, PRODUCT_DETAILS, PRICE_CONTRIBUTION, NEW_PRODUCT_REGISTRATION
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: Scan2ShopViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Scan2ShopApp(viewModel = viewModel) {
                    viewModel.toggleDarkMode()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Scan2ShopApp(
    viewModel: Scan2ShopViewModel,
    onToggleDark: () -> Unit
) {
    var activeTab by remember { mutableStateOf(TabMode.LIST) }
    var activeScreen by remember { mutableStateOf(ActiveScreen.NONE) }
    
    // Auxiliary states
    var detailProduct by remember { mutableStateOf<Product?>(null) }
    var missingBarcode by remember { mutableStateOf("") }
    
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val promptSetDefaultStore by viewModel.promptSetDefaultStore.collectAsState()

    if (promptSetDefaultStore != null) {
        val targetStore = promptSetDefaultStore!!
        Dialog(
            onDismissRequest = { viewModel.dismissDefaultStorePrompt() },
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Set Default Store?",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Text(
                        text = "You have selected $targetStore for multiple items on your shopping list. Would you like to set $targetStore as your default store? It will be pre-selected when you add products in the future.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.dismissDefaultStorePrompt()
                            }
                        ) {
                            Text("Not Now", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.setDefaultStore(targetStore)
                                viewModel.dismissDefaultStorePrompt()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Set as Default", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (activeScreen == ActiveScreen.NONE) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo_1780859186863),
                                contentDescription = "Scan2Shop Logo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                "Scan2Shop",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    },
                    actions = {
                        // Adaptive theme toggles in header (Section 5.2 requirement)
                        IconButton(
                            onClick = onToggleDark,
                            modifier = Modifier.testTag("dark_mode_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.border(
                        0.5.dp, 
                        Color.LightGray.copy(alpha = 0.2f)
                    )
                )
            }
        },
        bottomBar = {
            if (activeScreen == ActiveScreen.NONE) {
                NavigationBar(
                    modifier = Modifier
                        .testTag("bottom_navigation_bar")
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == TabMode.PRODUCTS,
                        onClick = { activeTab = TabMode.PRODUCTS },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Browse Catalog") },
                        label = { Text("Products", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_products")
                    )

                    NavigationBarItem(
                        selected = activeTab == TabMode.LIST,
                        onClick = { activeTab = TabMode.LIST },
                        icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "My List") },
                        label = { Text("List", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_list")
                    )

                    NavigationBarItem(
                        selected = activeTab == TabMode.SETTINGS,
                        onClick = { activeTab = TabMode.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeScreen == ActiveScreen.NONE && activeTab == TabMode.PRODUCTS) {
                FloatingActionButton(
                    onClick = { activeScreen = ActiveScreen.SCANNER },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("fab_scan_button")
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner, 
                        contentDescription = "Floating Scan Product Button",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        
        // Main Screen Navigation and Overlay transitions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(activeTab, activeScreen) {
                    if (activeScreen == ActiveScreen.NONE) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag > 150f) {
                                    // Swipe right (from left to right)
                                    if (activeTab == TabMode.LIST) {
                                        activeTab = TabMode.PRODUCTS
                                    } else if (activeTab == TabMode.SETTINGS) {
                                        activeTab = TabMode.LIST
                                    }
                                } else if (totalDrag < -150f) {
                                    // Swipe left (from right to left)
                                    if (activeTab == TabMode.PRODUCTS) {
                                        activeTab = TabMode.LIST
                                    } else if (activeTab == TabMode.LIST) {
                                        activeTab = TabMode.SETTINGS
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            }
                        )
                    }
                }
        ) {
            // Underlay Primary Feed
            when (activeTab) {
                TabMode.PRODUCTS -> {
                    ProductsBrowseScreen(
                        viewModel = viewModel,
                        onOpenProductDetails = { product ->
                            detailProduct = product
                            viewModel.selectProduct(product.id)
                            activeScreen = ActiveScreen.PRODUCT_DETAILS
                        }
                    )
                }
                TabMode.LIST -> {
                    ShoppingListScreen(
                        viewModel = viewModel,
                        onOpenProductDetails = { product ->
                            detailProduct = product
                            viewModel.selectProduct(product.id)
                            activeScreen = ActiveScreen.PRODUCT_DETAILS
                        },
                        onOpenScanner = {
                            activeScreen = ActiveScreen.SCANNER
                        }
                    )
                }
                TabMode.SETTINGS -> {
                    SettingsScreen()
                }
            }

            // High Fidelity Overlay Screen stack utilizing Spring animated transitions
            AnimatedVisibility(
                visible = activeScreen == ActiveScreen.SCANNER,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring()) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring()) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag > 150f) {
                                        activeScreen = ActiveScreen.NONE
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        }
                ) {
                    CameraBarcodeScanner(
                        viewModel = viewModel,
                        onProductFound = { product ->
                            detailProduct = product
                            viewModel.selectProduct(product.id)
                            activeScreen = ActiveScreen.PRODUCT_DETAILS
                        },
                        onNewProductBarcode = { code ->
                            missingBarcode = code
                            activeScreen = ActiveScreen.NEW_PRODUCT_REGISTRATION
                        },
                        onClose = {
                            activeScreen = ActiveScreen.NONE
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = activeScreen == ActiveScreen.PRODUCT_DETAILS && detailProduct != null,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = spring()) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring()) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                detailProduct?.let { pr ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pr) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > 150f) {
                                            activeScreen = ActiveScreen.NONE
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        totalDrag += dragAmount
                                    }
                                )
                            }
                    ) {
                        ProductDetailsScreen(
                            product = pr,
                            viewModel = viewModel,
                            onOpenContribution = {
                                viewModel.initPriceContribution(it)
                                activeScreen = ActiveScreen.PRICE_CONTRIBUTION
                            },
                            onClose = {
                                activeScreen = ActiveScreen.NONE
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = activeScreen == ActiveScreen.PRICE_CONTRIBUTION && detailProduct != null,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring()) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring()) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                detailProduct?.let { pr ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pr) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > 150f) {
                                            activeScreen = ActiveScreen.PRODUCT_DETAILS
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        totalDrag += dragAmount
                                    }
                                )
                            }
                    ) {
                        PriceContributionScreen(
                            product = pr,
                            viewModel = viewModel,
                            onBack = {
                                activeScreen = ActiveScreen.PRODUCT_DETAILS
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = activeScreen == ActiveScreen.NEW_PRODUCT_REGISTRATION && missingBarcode.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(missingBarcode) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag > 150f) {
                                        activeScreen = ActiveScreen.SCANNER
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        }
                ) {
                    RegisterNewProductScreen(
                        barcode = missingBarcode,
                        viewModel = viewModel,
                        onRegistrationSuccess = {
                            activeScreen = ActiveScreen.NONE
                        },
                        onCancel = {
                            activeScreen = ActiveScreen.SCANNER
                        }
                    )
                }
            }
        }
    }
}

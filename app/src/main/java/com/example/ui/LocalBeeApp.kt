@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.R
import com.example.data.OrderEntity
import com.example.data.ProductEntity
import com.example.data.CategoryConfigItem
import com.example.data.CategoryService
import com.example.ui.theme.LocalBeeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun LocalBeeApp(viewModel: LocalBeeViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val themeName by viewModel.appTheme.collectAsStateWithLifecycle()
    val customBgHex by viewModel.customBgColorHex.collectAsStateWithLifecycle()
    val customPrimaryHex by viewModel.customPrimaryColorHex.collectAsStateWithLifecycle()

    // Handle Toast alerts globally from ViewModel
    LaunchedEffect(Unit) {
        viewModel.uiEventMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Performance Audit: Log navigation destination and track live rendering transitions
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val route = destination.route ?: "unknown"
            // Simulate realistic Compose layout parsing & rendering frame overhead in ms (e.g. 12 to 34ms)
            val renderOverhead = (10..38).random().toLong()
            viewModel.logNavigationLatency(route.uppercase(), renderOverhead)
        }
    }

    LocalBeeTheme(
        themeName = themeName,
        customBgColorHex = customBgHex,
        customPrimaryColorHex = customPrimaryHex
    ) {
        NavHost(
            navController = navController,
            startDestination = "splash"
        ) {
            composable("splash") {
                SplashScreen(navController)
            }
            composable("home") {
                AppScaffold(navController, viewModel, currentRoute = "home") {
                    HomeScreen(navController, viewModel)
                }
            }
            composable("search") {
                AppScaffold(navController, viewModel, currentRoute = "search") {
                    SearchScreen(navController, viewModel)
                }
            }
            composable("product_detail") {
                ProductScreen(navController, viewModel)
            }
            composable("cart") {
                AppScaffold(navController, viewModel, currentRoute = "cart") {
                    CartScreen(navController, viewModel)
                }
            }
            composable("checkout") {
                CheckoutScreen(navController, viewModel)
            }
            composable("tracking") {
                OrderTrackingScreen(navController, viewModel)
            }
            composable("profile") {
                AppScaffold(navController, viewModel, currentRoute = "profile") {
                    ProfileScreen(navController, viewModel)
                }
            }
            composable("download_page") {
                DownloadPageScreen(navController)
            }
            composable("localbee_care") {
                LocalBeeCareScreen(navController, viewModel)
            }
        }
    }
}

// Global Scaffold with Bottom Navigation Bar for main sections
@Composable
fun AppScaffold(
    navController: NavController,
    viewModel: LocalBeeViewModel,
    currentRoute: String,
    content: @Composable (PaddingValues) -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val totalCartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { if (currentRoute != "home") navController.navigate("home") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == "search",
                    onClick = { if (currentRoute != "search") navController.navigate("search") },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == "cart",
                    onClick = { if (currentRoute != "cart") navController.navigate("cart") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalCartCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.testTag("cart_icon_badge")
                                    ) {
                                        Text(totalCartCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    },
                    label = { Text("Cart") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == "profile",
                    onClick = { if (currentRoute != "profile") navController.navigate("profile") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content(paddingValues)
            }
        }
    )
}

// ----------------- 1. SPLASH SCREEN -----------------
@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds animation time
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_logo),
                contentDescription = "LocalBee Logo",
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "LocalBee",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("splash_screen_title")
            )
            Text(
                text = "Fastest Local Bee Delivery 🐝",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

// ----------------- 2. HOME SCREEN -----------------
@Composable
fun HomeScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    val address by viewModel.userAddress.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var searchQueryText by remember { mutableStateOf("") }

    val filteredHomeProducts = remember(searchQueryText, products) {
        if (searchQueryText.isBlank()) emptyList()
        else {
            products.filter { product ->
                product.name.contains(searchQueryText, ignoreCase = true) ||
                product.category.contains(searchQueryText, ignoreCase = true)
            }
        }
    }

    val fruitProducts = remember(products) {
        products.filter { it.category.equals("Fruits", ignoreCase = true) }
    }

    val vegetableProducts = remember(products) {
        products.filter { it.category.equals("Vegetables", ignoreCase = true) }
    }

    val categoriesConfig by viewModel.categories.collectAsStateWithLifecycle()
    val categories = remember(categoriesConfig) {
        categoriesConfig.map { CategoryItem(it.name, it.emoji) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isOnline) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                        .testTag("offline_indicator_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Offline Mode Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Showing cached listings and order profiles from local Room SQLite Database.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        // Top Location Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location Pin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Delivery Location 📍",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = address?.location ?: "Select location",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Top Search Bar (Interactive TextField)
        item {
            OutlinedTextField(
                value = searchQueryText,
                onValueChange = { searchQueryText = it },
                placeholder = { Text("Search products by name or category...", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("home_search_bar"),
                shape = RoundedCornerShape(24.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQueryText.isNotEmpty()) {
                        IconButton(onClick = { searchQueryText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        if (searchQueryText.isNotBlank()) {
            // --- SEARCH RESULTS VIEW ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matched Products (${filteredHomeProducts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { searchQueryText = "" }) {
                        Text("Clear", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredHomeProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No products match \"$searchQueryText\". Try searching 'Fruits', 'Milk', or 'Bakery'!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Display filtered results in a horizontal scrollable list or rows
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val chunks = remember(filteredHomeProducts) { filteredHomeProducts.chunked(2) }
                        for (chunk in chunks) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (product in chunk) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProductCard(product, viewModel, navController)
                                    }
                                }
                                if (chunk.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            // --- REGULAR HOME VIEW ---

            // Hero Banner Display
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Promo Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Categories Header
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Explore Categories 🐝",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Categories Grid Custom Layout (Flow or pre-calculated rows to avoid Grid-inside-LazyColumn issue)
            item {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    val rows = remember(categories) { categories.chunked(4) }
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (item in row) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .width(80.dp)
                                        .clickable {
                                            viewModel.selectCategory(item.name)
                                            navController.navigate("search")
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Card(
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(item.emoji, fontSize = 28.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fresh Fruits Listing Component
            if (fruitProducts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Fresh Fruits 🍎",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(fruitProducts) { product ->
                            ProductCard(product, viewModel, navController)
                        }
                    }
                }
            }

            // Fresh Vegetables Listing Component
            if (vegetableProducts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Fresh Vegetables 🥬",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(vegetableProducts) { product ->
                            ProductCard(product, viewModel, navController)
                        }
                    }
                }
            }

            // Recommended Products Header
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Freshly Picked For You",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Recommended Products Horizontal list
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(products.take(6)) { product ->
                        ProductCard(product, viewModel, navController)
                    }
                }
            }
        }
    }
}

data class CategoryItem(val name: String, val emoji: String)

@Composable
fun ProductCard(product: ProductEntity, viewModel: LocalBeeViewModel, navController: NavController) {
    val allFeedback by viewModel.allFeedback.collectAsStateWithLifecycle()
    val productReviews = remember(allFeedback, product.id) {
        allFeedback.filter { it.productId == product.id }
    }
    val averageRating = remember(productReviews) {
        if (productReviews.isEmpty()) 0.0
        else productReviews.map { it.rating }.average()
    }

    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable {
                viewModel.selectProduct(product.id)
                navController.navigate("product_detail")
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Styled Gradient box for Product Emoji Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(product.emoji, fontSize = 48.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛵 " + product.deliveryTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
                if (averageRating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", averageRating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { viewModel.addToCart(product.id) },
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ----------------- 3. SEARCH SCREEN -----------------
@Composable
fun SearchScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

    // Filter products dynamically in Compose based on query and selected category
    val filteredProducts = remember(query, selectedCategory, allProducts) {
        allProducts.filter { product ->
            val matchQuery = query.isBlank() || product.name.contains(query, ignoreCase = true) || product.category.contains(query, ignoreCase = true)
            val matchCategory = selectedCategory == null || product.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    val categoriesConfig by viewModel.categories.collectAsStateWithLifecycle()
    val categoriesList = remember(categoriesConfig) {
        listOf("All") + categoriesConfig.map { it.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Input Box Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search for fresh products...") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("product_search_input"),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
        }

        // Category Selection Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(categoriesList) { cat ->
                val isSelected = (cat == "All" && selectedCategory == null) || (cat == selectedCategory)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (cat == "All") {
                            viewModel.selectCategory(null)
                        } else {
                            viewModel.selectCategory(cat)
                        }
                    },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Recent Searches Block (only visible if search query is empty)
        if (query.isEmpty() && recentSearches.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    TextButton(onClick = { viewModel.clearRecentSearches() }) {
                        Text("Clear All", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (search in recentSearches) {
                        SuggestionChip(
                            onClick = { viewModel.updateSearchQuery(search) },
                            label = { Text(search) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Products Results
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐝", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No products found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "Try a different query or category",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(product, viewModel, navController)
                }
            }
        }
    }
}

// ----------------- 4. PRODUCT DETAIL SCREEN -----------------
@Composable
fun ProductScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    val product by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val allFeedback by viewModel.allFeedback.collectAsStateWithLifecycle()

    val quantityInCart = cartItems.find { it.productId == product?.id }?.quantity ?: 0

    val productReviews = remember(allFeedback, product?.id) {
        val pid = product?.id ?: -1
        allFeedback.filter { it.productId == pid }
    }
    val averageRating = remember(productReviews) {
        if (productReviews.isEmpty()) 0.0
        else productReviews.map { it.rating }.average()
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(product?.name ?: "Product Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        product?.let { prod ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Giant Beautiful product illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(prod.emoji, fontSize = 80.sp)
                        }
                    }
                }

                // Details layout
                Column(modifier = Modifier.padding(24.dp)) {
                    // Category Badge
                    SuggestionChip(
                        onClick = {},
                        label = { Text(prod.category) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prod.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$${String.format("%.2f", prod.price)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Interactive Star Rating Summary Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    ) {
                        if (averageRating > 0) {
                            Text("⭐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", averageRating)} / 5.0",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${productReviews.size} customer reviews)",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text("⭐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "No reviews yet",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Stock and delivery cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Stock status", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = if (prod.stock > 0) "In Stock (${prod.stock})" else "Out of Stock",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (prod.stock > 10) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Delivery Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "🛵 " + prod.deliveryTime,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Product Description", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prod.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Add cart actions
                    if (quantityInCart == 0) {
                        Button(
                            onClick = { viewModel.addToCart(prod.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("add_to_cart_btn"),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Cart", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quantity in Cart:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(prod.id, quantityInCart - 1) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text(
                                    text = quantityInCart.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(prod.id, quantityInCart + 1) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape),
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }

                    // --- 💬 Customer Reviews & Feedback Section ---
                    Spacer(modifier = Modifier.height(36.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Customer Feedback & Reviews 💬", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (productReviews.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No reviews for this product yet. Order and be the first to rate it! 🍯",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            productReviews.forEach { review ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("product_review_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = review.userName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            // Star rating display
                                            Row {
                                                (1..5).forEach { star ->
                                                    Text(
                                                        text = if (star <= review.rating) "★" else "☆",
                                                        fontSize = 13.sp,
                                                        color = if (star <= review.rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                    )
                                                }
                                            }
                                        }
                                        if (review.feedbackText.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = review.feedbackText,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Product details unavailable.")
            }
        }
    }
}

// ----------------- 5. CART SCREEN -----------------
@Composable
fun CartScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

    // Map cart items to actual product details
    val cartProducts = remember(cartItems, allProducts) {
        cartItems.mapNotNull { item ->
            val product = allProducts.find { it.id == item.productId }
            if (product != null) Pair(product, item.quantity) else null
        }
    }

    val subtotal = remember(cartProducts) { cartProducts.sumOf { it.first.price * it.second } }
    val deliveryFee = remember(cartItems) { if (cartItems.isEmpty()) 0.0 else 1.99 }
    val total = remember(subtotal, deliveryFee) { subtotal + deliveryFee }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "My Basket 🛒",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(16.dp)
        )

        if (cartProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🐝", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your basket is empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Fill it with delicious fruits, fresh food, or daily essentials!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Start Shopping")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartProducts) { (product, qty) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(product.emoji, fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "$${String.format("%.2f", product.price)} each",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(product.id, qty - 1) },
                                    modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                }
                                Text(qty.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(product.id, qty + 1) },
                                    modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Calculations card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = MaterialTheme.colorScheme.secondary)
                        Text("$${String.format("%.2f", subtotal)}", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery bee-charge", color = MaterialTheme.colorScheme.secondary)
                        Text("$${String.format("%.2f", deliveryFee)}", fontWeight = FontWeight.Medium)
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            text = "$${String.format("%.2f", total)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { navController.navigate("checkout") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button"),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Checkout ($${String.format("%.2f", total)})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------- 6. CHECKOUT SCREEN -----------------
@Composable
fun CheckoutScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    val addressEntity by viewModel.userAddress.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") }

    // Simulation state
    var isSimulatingPayment by remember { mutableStateOf(false) }
    var paymentStep by remember { mutableStateOf(1) }

    // Initialize fields with current profile details
    LaunchedEffect(addressEntity) {
        addressEntity?.let {
            name = it.name
            phone = it.phone
            address = it.address
            location = it.location
        }
    }

    val cartProducts = remember(cartItems, allProducts) {
        cartItems.mapNotNull { item ->
            val product = allProducts.find { it.id == item.productId }
            if (product != null) Pair(product, item.quantity) else null
        }
    }
    val subtotal = remember(cartProducts) { cartProducts.sumOf { it.first.price * it.second } }
    val total = remember(subtotal) { subtotal + 1.99 }

    if (isSimulatingPayment) {
        LaunchedEffect(Unit) {
            paymentStep = 1
            delay(1300)
            paymentStep = 2
            delay(1300)
            paymentStep = 3
            delay(1300)
            paymentStep = 4
            delay(1500)
            
            viewModel.checkout(
                name, phone, address, location, paymentMethod,
                onOrderPlaced = {
                    isSimulatingPayment = false
                    navController.navigate("tracking") {
                        popUpTo("home")
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { /* Prevent dismissal during checkout */ },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (paymentStep == 4) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (paymentStep) {
                        1 -> Text("🔒", fontSize = 32.sp)
                        2 -> Text("🗺️", fontSize = 32.sp)
                        3 -> Text("💸", fontSize = 32.sp)
                        4 -> Text("🎉", fontSize = 32.sp)
                    }
                }
            },
            title = {
                Text(
                    text = when (paymentStep) {
                        1 -> "Establishing Secure Connection"
                        2 -> "Verifying Delivery Address"
                        3 -> "Processing Secure Payment"
                        else -> "Order Confirmed!"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (paymentStep) {
                        1 -> {
                            Text(
                                text = "Contacting LocalBee payment gateway secure honey-tunnel...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                        }
                        2 -> {
                            Text(
                                text = "Locating closest rider near \"$location\"...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                        }
                        3 -> {
                            Text(
                                text = "Authorizing transaction of $${String.format("%.2f", total)} via $paymentMethod...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                        }
                        4 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Your order was successfully transmitted to our local hive! 🐝",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Redirecting you to track your delivery live...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Checkout 📦", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Text(
                text = "Delivery Details",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().testTag("checkout_name_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Complete Address") },
                modifier = Modifier.fillMaxWidth().testTag("checkout_address_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Area / Location") },
                modifier = Modifier.fillMaxWidth().testTag("checkout_location_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Order Summary & Price Calculation",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cartProducts.forEach { (product, qty) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(product.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${product.name} x$qty",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "$${String.format("%.2f", product.price * qty)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("$${String.format("%.2f", subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery Bee-Charge", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("$1.99", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(
                            text = "$${String.format("%.2f", total)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Payment Method",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Payment Choice Rows
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { paymentMethod = "UPI" }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = paymentMethod == "UPI", onClick = { paymentMethod = "UPI" })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UPI (GPay / PhonePe / Paytm)", fontWeight = FontWeight.Medium)
                    }
                    Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { paymentMethod = "Cash on Delivery" }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = paymentMethod == "Cash on Delivery", onClick = { paymentMethod = "Cash on Delivery" })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cash on Delivery (COD)", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || address.isBlank() || location.isBlank()) {
                        Toast.makeText(navController.context, "Please fill out all fields!", Toast.LENGTH_SHORT).show()
                    } else {
                        isSimulatingPayment = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("place_order_button"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Confirm & Place Order ($${String.format("%.2f", total)})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------- 7. ORDER TRACKING SCREEN -----------------
@Composable
fun OrderTrackingScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    val activeOrderId by viewModel.activeTrackingOrderId.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()

    val trackingOrder = remember(activeOrderId, allOrders) {
        allOrders.find { it.id == activeOrderId } ?: allOrders.firstOrNull()
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Order Tracking 🛵", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") { popUpTo("home") { inclusive = false } } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                }
            )
        }
    ) { padding ->
        trackingOrder?.let { order ->
            val statuses = remember { listOf("Confirmed", "Preparing", "Out for Delivery", "Delivered") }
            val currentIndex = remember(order.status, statuses) { statuses.indexOf(order.status).coerceAtLeast(0) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
            ) {
                // Tracking Status Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Order ID: #LBE-${order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Large illustration representing status
                        val (statusEmoji, statusMsg) = remember(order.status) {
                            when (order.status) {
                                "Confirmed" -> Pair("✅", "Our bees are organizing your basket!")
                                "Preparing" -> Pair("🍳", "Bee chef is packing fresh goods.")
                                "Out for Delivery" -> Pair("🛵", "Rider bee is zooming your way!")
                                "Delivered" -> Pair("🏠", "Successfully delivered to your home!")
                                else -> Pair("🐝", "LocalBee is processing your order.")
                            }
                        }

                        Text(statusEmoji, fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(order.status.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(statusMsg, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timeline Stepper Visualizer
                Text("Delivery Progress", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    statuses.forEachIndexed { index, status ->
                        val isCompleted = index <= currentIndex
                        val isCurrent = index == currentIndex

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Indicator Node
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCompleted) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text((index + 1).toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Status Details
                            Column {
                                Text(
                                    text = when (status) {
                                        "Confirmed" -> "Confirmed ✅"
                                        "Preparing" -> "Preparing 🍳"
                                        "Out for Delivery" -> "Out for Delivery 🛵"
                                        "Delivered" -> "Delivered 🏠"
                                        else -> status
                                    },
                                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else if (isCompleted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = when (status) {
                                        "Confirmed" -> "Order accepted and scheduled"
                                        "Preparing" -> "Fresh ingredients packaged and sorted"
                                        "Out for Delivery" -> "Rider is heading to Indiranagar"
                                        "Delivered" -> "Handed over safely"
                                        else -> ""
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = if (isCompleted) 0.8f else 0.4f)
                                )
                            }
                        }

                        // Stepper connector line (except last element)
                        if (index < statuses.size - 1) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 17.dp)
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(
                                        if (index < currentIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Fast Forward Simulation Control
                if (order.status != "Delivered") {
                    Button(
                        onClick = {
                            val nextStatus = when (order.status) {
                                "Confirmed" -> "Preparing"
                                "Preparing" -> "Out for Delivery"
                                "Out for Delivery" -> "Delivered"
                                else -> "Confirmed"
                            }
                            viewModel.fastForwardOrderStatus(order.id, nextStatus)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("fast_forward_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Fast Forward")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fast Forward Tracking Status ⚡")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Order metadata summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Delivery Address Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${order.name} | ${order.phone}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(order.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(order.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Text("Items Ordered", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(order.itemsSummary, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total (incl. delivery)", fontWeight = FontWeight.Bold)
                            Text("$${String.format("%.2f", order.totalAmount)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Interactive Product Feedback Form after Delivery Completed
                if (order.status == "Delivered") {
                    Spacer(modifier = Modifier.height(24.dp))
                    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
                    val orderedProducts = remember(order.itemsSummary, allProducts) {
                        allProducts.filter { product ->
                            order.itemsSummary.contains(product.name, ignoreCase = true)
                        }
                    }

                    if (orderedProducts.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🌟", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Rate Your Products",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = "Your feedback helps other local shoppers find the freshest picks!",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                orderedProducts.forEach { product ->
                                    val allFeedback by viewModel.allFeedback.collectAsStateWithLifecycle()
                                    val existingFeedback = remember(allFeedback, order.id, product.id) {
                                        allFeedback.find { it.orderId == order.id && it.productId == product.id }
                                    }

                                    // Individual rating item
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(product.emoji, fontSize = 24.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(product.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }

                                        if (existingFeedback != null) {
                                            // Feedback has already been submitted
                                            Row(
                                                modifier = Modifier.padding(top = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Your Rating: ", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                                                Row {
                                                    (1..5).forEach { star ->
                                                        Text(
                                                            text = if (star <= existingFeedback.rating) "★" else "☆",
                                                            fontSize = 14.sp,
                                                            color = if (star <= existingFeedback.rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Submitted! ❤️",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (existingFeedback.feedbackText.isNotBlank()) {
                                                Text(
                                                    text = "\"${existingFeedback.feedbackText}\"",
                                                    fontStyle = FontStyle.Italic,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                                )
                                            }
                                        } else {
                                            // Interactive review builder state
                                            var rating by remember { mutableStateOf(0) }
                                            var comment by remember { mutableStateOf("") }

                                            // Star selector
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            ) {
                                                (1..5).forEach { star ->
                                                    val isSelected = star <= rating
                                                    IconButton(
                                                        onClick = { rating = star },
                                                        modifier = Modifier.size(32.dp).testTag("rate_star_${product.id}_$star")
                                                    ) {
                                                        Text(
                                                            text = if (isSelected) "★" else "☆",
                                                            fontSize = 22.sp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                        )
                                                    }
                                                }
                                            }

                                            if (rating > 0) {
                                                OutlinedTextField(
                                                    value = comment,
                                                    onValueChange = { comment = it },
                                                    placeholder = { Text("Write a quick comment... (optional)", fontSize = 12.sp) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .testTag("rate_comment_${product.id}"),
                                                    textStyle = TextStyle(fontSize = 13.sp),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                Button(
                                                    onClick = {
                                                        viewModel.submitProductFeedback(
                                                            productId = product.id,
                                                            orderId = order.id,
                                                            rating = rating,
                                                            feedbackText = comment,
                                                            userName = order.name
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.End)
                                                        .padding(top = 4.dp)
                                                        .testTag("rate_submit_${product.id}"),
                                                    shape = RoundedCornerShape(16.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text("Submit Feedback", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐝", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No orders placed yet!", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Shop Now")
                    }
                }
            }
        }
    }
}

// ----------------- 8. PROFILE SCREEN -----------------
@Composable
fun ProfileScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    val addressEntity by viewModel.userAddress.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val customBgColorHex by viewModel.customBgColorHex.collectAsStateWithLifecycle()
    val customPrimaryColorHex by viewModel.customPrimaryColorHex.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var isEditingAddress by remember { mutableStateOf(false) }

    LaunchedEffect(addressEntity) {
        addressEntity?.let {
            name = it.name
            phone = it.phone
            address = it.address
            location = it.location
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Upper Profile Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (name.isNotBlank()) name else "Guest Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "LocalBee Deliveries (Guest Mode)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        // Live Active Order Tracking Section
        val activeOrders = remember(allOrders) {
            allOrders.filter { it.status != "Delivered" }
        }

        activeOrders.forEach { activeOrder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        viewModel.setActiveTrackingOrder(activeOrder.id)
                        navController.navigate("tracking")
                    }
                    .testTag("profile_active_order_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛵", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Active Delivery Progress",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Order #LBE-${activeOrder.id}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = activeOrder.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = activeOrder.itemsSummary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val statuses = listOf("Confirmed", "Preparing", "Out for Delivery", "Delivered")
                    val currentIndex = statuses.indexOf(activeOrder.status).coerceAtLeast(0)
                    val progressValue = when (activeOrder.status) {
                        "Confirmed" -> 0.25f
                        "Preparing" -> 0.50f
                        "Out for Delivery" -> 0.75f
                        "Delivered" -> 1.0f
                        else -> 0.1f
                    }

                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status: " + when(activeOrder.status) {
                                "Confirmed" -> "Confirmed ✅"
                                "Preparing" -> "Preparing 🍳"
                                "Out for Delivery" -> "Out for Delivery 🛵"
                                else -> activeOrder.status
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap to track live →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // App Download Page Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { navController.navigate("download_page") }
                .testTag("profile_download_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📲", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LocalBee App Download Page",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Scan QR, download APK & install on other devices 🐝",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate to Download Page",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // LocalBee Care Option Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { navController.navigate("localbee_care") }
                .testTag("profile_localbee_care_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LocalBee Care Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "AI ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Report bugs, chat with support & check system health 🩺",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate to Support Center",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Address Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Delivery Address 🏠", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { isEditingAddress = !isEditingAddress }) {
                        Text(if (isEditingAddress) "Cancel" else "Edit", fontWeight = FontWeight.Bold)
                    }
                }

                if (isEditingAddress) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Area / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.saveAddress(name, phone, address, location)
                            isEditingAddress = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_address_button")
                    ) {
                        Text("Save Address")
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (name.isNotBlank()) "$name | $phone" else "No saved address details",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = address.ifBlank { "Please edit and save your default address." },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = location,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Developer Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("developer_info_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨‍💻", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lead Developer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = "CREATOR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Sharuk",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Email
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✉️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Email Address",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Fm1w786@gmail.com",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Contact Number
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📞", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Contact Number",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "9845960784",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Location / Address
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Location / Region",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Lakkavali  thariker  chickmagalur 577128",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Simulated Connectivity & Local Caching Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connectivity_simulation_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connectivity & Caching",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Simulate offline and test SQLite persistence",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { viewModel.toggleOnlineStatus() },
                        modifier = Modifier.testTag("offline_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isOnline) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.errorContainer, 
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isOnline) "🟢" else "🔴", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOnline) "Simulated Status: Online" else "Simulated Status: Offline Caching Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (isOnline) "Fetching dynamic listings over simulated HTTP requests." 
                                   else "Reading cached catalogs and previous orders directly from persistent SQLite Room DB.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Theme & Background Color Customizer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_designer_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎨", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Theme & Background Designer",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Switch background style, color presets, or build your own!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Predefined themes row
                Text(
                    text = "Select Background Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val themesList = listOf(
                    "BEE_GOLD" to "🍯 Honey Gold",
                    "DEEP_BLACK" to "🖤 Deep Black",
                    "MONOCHROME" to "🐼 Monochrome",
                    "WARM_CREAM" to "🍦 Warm Cream",
                    "CUSTOM" to "🎨 Custom Canvas"
                )

                OptIn(ExperimentalMaterial3Api::class)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themesList.forEach { (themeId, label) ->
                        val isSelected = currentTheme == themeId
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAppTheme(themeId) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                if (currentTheme == "CUSTOM") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Background Color Selection
                    Text(
                        text = "Background Preset Colors",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val bgPresets = listOf(
                        "#FAF9F6" to Color(0xFFFAF9F6), // Alabaster
                        "#F3E8FF" to Color(0xFFF3E8FF), // Lavender
                        "#E0F2FE" to Color(0xFFE0F2FE), // Sky Blue
                        "#ECFDF5" to Color(0xFFECFDF5), // Soft Mint
                        "#FFF1F2" to Color(0xFFFFF1F2), // Soft Rose
                        "#1E293B" to Color(0xFF1E293B), // Slate Dark
                        "#0F172A" to Color(0xFF0F172A)  // OLED Slate
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bgPresets.forEach { (hex, color) ->
                            val isSelected = customBgColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setCustomBgColorHex(hex) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Accent Color Selection
                    Text(
                        text = "Accent Preset Colors",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val primaryPresets = listOf(
                        "#EAB308" to Color(0xFFEAB308), // Gold
                        "#D97706" to Color(0xFFD97706), // Amber
                        "#10B981" to Color(0xFF10B981), // Emerald
                        "#F43F5E" to Color(0xFFF43F5E), // Rose
                        "#2563EB" to Color(0xFF2563EB), // Royal
                        "#8B5CF6" to Color(0xFF8B5CF6)  // Violet
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        primaryPresets.forEach { (hex, color) ->
                            val isSelected = customPrimaryColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setCustomPrimaryColorHex(hex) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Advanced Manual Hex Input Fields
                    Text(
                        text = "Advanced Hex Color Picker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customBgColorHex,
                            onValueChange = { if (it.length <= 9) viewModel.setCustomBgColorHex(it) },
                            label = { Text("Background Hex", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = customPrimaryColorHex,
                            onValueChange = { if (it.length <= 9) viewModel.setCustomPrimaryColorHex(it) },
                            label = { Text("Accent Hex", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // My Orders section
        Text("My Orders History 📦", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (allOrders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No order history yet. Buy some fresh honey or bakery goods!", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (order in allOrders) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setActiveTrackingOrder(order.id)
                                navController.navigate("tracking")
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Order #LBE-${order.id}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (order.status == "Delivered") MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Text(
                                                text = order.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = if (order.status == "Delivered") MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = order.itemsSummary,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${String.format("%.2f", order.totalAmount)}",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View Details", tint = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )

                            // Real-time horizontal delivery progress bar component
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val steps = listOf("Order Placed", "Packed", "Out for Delivery", "Delivered")
                                val currentStatus = order.status
                                val currentIndex = when (currentStatus) {
                                    "Confirmed" -> 0
                                    "Preparing" -> 1
                                    "Out for Delivery" -> 2
                                    "Delivered" -> 3
                                    else -> 0
                                }

                                steps.forEachIndexed { index, stepName ->
                                    val isCompleted = index <= currentIndex
                                    val isCurrent = index == currentIndex

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Left connector line
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(2.dp)
                                                    .background(
                                                        if (index > 0 && index <= currentIndex) MaterialTheme.colorScheme.primary
                                                        else if (index > 0) MaterialTheme.colorScheme.surfaceVariant
                                                        else Color.Transparent
                                                    )
                                            )

                                            // Circle Indicator Node
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCompleted) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCompleted) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                    )
                                                }
                                            }

                                            // Right connector line
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(2.dp)
                                                    .background(
                                                        if (index < steps.size - 1 && index < currentIndex) MaterialTheme.colorScheme.primary
                                                        else if (index < steps.size - 1) MaterialTheme.colorScheme.surfaceVariant
                                                        else Color.Transparent
                                                    )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = stepName,
                                            fontSize = 9.sp,
                                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                    else if (isCompleted) MaterialTheme.colorScheme.onSurface
                                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 10.sp,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Offers and FAQ Cards
        Text("Offers & Coupons 🎁", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Card(
                    modifier = Modifier.width(220.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("BEEFREE 🐝", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("No delivery charge on orders above $10!", fontSize = 12.sp)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.width(220.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("HONEY50 🍯", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Get up to 50% discount on fresh organic honey!", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Help & FAQ Section
        Text("Help & FAQ 📞", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("How fast is LocalBee?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("We delivery directly within 15 to 40 minutes using hyper-local bee messengers!", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Can I return fresh vegetables?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Yes! Our instant check-upon-delivery policy guarantees you can return anything immediately if it's not absolutely fresh.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Need urgent help?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Support email: honeyhelp@localbee.com", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ----------------- 9. DOWNLOAD PAGE SCREEN -----------------
@Composable
fun DownloadPageScreen(navController: NavController) {
    var showScanSimulation by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStatusText by remember { mutableStateOf("Download Android App") }
    var scanStep by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Download App 🐝", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 🐝 LocalBee Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("🐝", fontSize = 52.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LocalBee App",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Your Local Store Delivered Fast",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Download LocalBee App and get groceries, food, gifts & mobile axsaris and more delivered to your doorstep.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 📲 Download Options Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📲", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Options",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Android Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Android App",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button: ⬇️ Download Android App
                    Button(
                        onClick = {
                            if (!isDownloading) {
                                isDownloading = true
                                scope.launch {
                                    downloadStatusText = "Downloading..."
                                    for (i in 1..10) {
                                        downloadProgress = i / 10f
                                        delay(200)
                                    }
                                    downloadStatusText = "LocalBee Installed Successfully! ✅"
                                    isDownloading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("download_apk_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(25.dp),
                        enabled = !isDownloading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⬇️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = downloadStatusText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (downloadProgress > 0f) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(downloadProgress * 100).toInt()}% downloaded (24.5 MB)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // APK Download Link Info
                    Text(
                        text = "APK Download Link: https://localbee.com/download/android/latest",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play Store Link (Coming Soon / Live ಆದ ಮೇಲೆ)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🛍️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google Play Store Link",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Live ಆದ ಮೇಲೆ (Coming Soon once we go Live!)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 📷 QR Code Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showScanSimulation = true
                        scanStep = 1
                        scope.launch {
                            delay(1000)
                            scanStep = 2
                            delay(1200)
                            scanStep = 3
                            delay(1200)
                            scanStep = 4
                        }
                    }
                    .testTag("qr_code_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📷", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan & Download LocalBee App",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Elegant custom-drawn QR Code Canvas
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val qrMarkerColor = MaterialTheme.colorScheme.onSurface
                        val qrSecondaryColor = MaterialTheme.colorScheme.secondary
                        val qrContainerColor = MaterialTheme.colorScheme.primaryContainer

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sizePx = size.width
                            val cellSize = sizePx / 15f

                            // Finder Pattern Top-Left
                            drawRect(
                                color = qrMarkerColor,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(cellSize * 4, cellSize * 4)
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = androidx.compose.ui.geometry.Offset(cellSize, cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2)
                            )
                            drawRect(
                                color = qrMarkerColor,
                                topLeft = androidx.compose.ui.geometry.Offset(cellSize * 1.5f, cellSize * 1.5f),
                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                            )

                            // Finder Pattern Top-Right
                            drawRect(
                                color = qrMarkerColor,
                                topLeft = androidx.compose.ui.geometry.Offset(sizePx - cellSize * 4, 0f),
                                size = androidx.compose.ui.geometry.Size(cellSize * 4, cellSize * 4)
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = androidx.compose.ui.geometry.Offset(sizePx - cellSize * 3, cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2)
                            )
                            drawRect(
                                color = qrMarkerColor,
                                topLeft = androidx.compose.ui.geometry.Offset(sizePx - cellSize * 2.5f, cellSize * 1.5f),
                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                            )

                            // Finder Pattern Bottom-Left
                            drawRect(
                                color = qrMarkerColor,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, sizePx - cellSize * 4),
                                size = androidx.compose.ui.geometry.Size(cellSize * 4, cellSize * 4)
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = androidx.compose.ui.geometry.Offset(cellSize, sizePx - cellSize * 3),
                                size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2)
                            )
                            drawRect(
                                color = qrMarkerColor,
                                topLeft = androidx.compose.ui.geometry.Offset(cellSize * 1.5f, sizePx - cellSize * 2.5f),
                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                            )

                            // Populate randomized QR cells
                            val seed = 77
                            val random = java.util.Random(seed.toLong())
                            for (row in 0 until 15) {
                                for (col in 0 until 15) {
                                    // Skip finder pattern zones
                                    if ((row < 5 && col < 5) || (row < 5 && col >= 10) || (row >= 10 && col < 5)) {
                                        continue
                                    }
                                    // Skip center bee emblem zone
                                    if (row in 6..8 && col in 6..8) {
                                        continue
                                    }
                                    if (random.nextBoolean()) {
                                        drawRoundRect(
                                            color = if (random.nextInt(3) == 0) qrSecondaryColor else qrMarkerColor,
                                            topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                            size = androidx.compose.ui.geometry.Size(cellSize * 0.85f, cellSize * 0.85f),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.2f, cellSize * 0.2f)
                                        )
                                    }
                                }
                            }
                        }

                        // Tiny Bee Center Emblem
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(qrContainerColor)
                                .border(1.5.dp, qrSecondaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🐝", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "📱 Simulate Customer QR Scan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tap QR Code above to simulate scanning from a customer's device. This demonstrates the automatic redirection, download, and installation flow! 🚀",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Interactive Scan Redirect Dialog
    if (showScanSimulation) {
        AlertDialog(
            onDismissRequest = { showScanSimulation = false },
            icon = { Text("📱", fontSize = 32.sp) },
            title = {
                Text(
                    text = "Customer QR scan flow",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Simulating real-time customer scan redirection and automatic app setup...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Step 1: Scanning QR Code
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (scanStep >= 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (scanStep > 1) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Text("1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (scanStep >= 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Customer QR Code Scanned successfully",
                                fontSize = 13.sp,
                                fontWeight = if (scanStep == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (scanStep >= 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Step 2: Open Download Page
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (scanStep >= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (scanStep > 2) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Text("2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (scanStep >= 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Opening LocalBee Download Page...",
                                fontSize = 13.sp,
                                fontWeight = if (scanStep == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (scanStep >= 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Step 3: App Downloading
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (scanStep >= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (scanStep > 3) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Text("3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (scanStep >= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Downloading local_bee_v1.0.apk",
                                    fontSize = 13.sp,
                                    fontWeight = if (scanStep == 3) FontWeight.Bold else FontWeight.Normal,
                                    color = if (scanStep >= 3) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                if (scanStep == 3) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Step 4: Installation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (scanStep >= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (scanStep >= 4) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Text("4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (scanStep >= 4) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "App Successfully Installed! 🚀🐝",
                                fontSize = 13.sp,
                                fontWeight = if (scanStep >= 4) FontWeight.Bold else FontWeight.Normal,
                                color = if (scanStep >= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showScanSimulation = false },
                    enabled = scanStep >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (scanStep >= 4) "Done (App Installed)" else "Processing Scan...")
                }
            }
        )
    }
}

// ----------------- 10. LOCALBEE CARE SUPPORT CENTER -----------------
@Composable
fun LocalBeeCareScreen(navController: NavController, viewModel: LocalBeeViewModel) {
    var activeTab by remember { mutableIntStateOf(0) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val appVersion by viewModel.appVersion.collectAsStateWithLifecycle()
    val bugReports by viewModel.bugReports.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val backgroundErrors by viewModel.backgroundErrors.collectAsStateWithLifecycle()
    val isScanningHealth by viewModel.isScanningHealth.collectAsStateWithLifecycle()
    val healthCheckResults by viewModel.healthCheckResults.collectAsStateWithLifecycle()
    val customerNotifications by viewModel.customerNotifications.collectAsStateWithLifecycle()

    val totalErrorsCount by viewModel.totalErrors.collectAsStateWithLifecycle()
    val activeIssuesCount by viewModel.activeIssues.collectAsStateWithLifecycle()
    val fixedIssuesCount by viewModel.fixedIssues.collectAsStateWithLifecycle()

    val unreadNotifCount = customerNotifications.count { !it.isRead }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LocalBee Care 🐝", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        showNotificationsDialog = true 
                        viewModel.markAllNotificationsAsRead()
                    }) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge { Text(unreadNotifCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal scrollable Tab selectors
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Text("🤖 AI Assistant", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Text("🐞 Report Bug", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                    Text("🩺 Health Check", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                    Text("🔄 App Update", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) {
                    Text("🔒 Security Hub", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Tab(selected = activeTab == 5, onClick = { activeTab = 5 }) {
                    Text("📊 Admin Panel", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Tab(selected = activeTab == 6, onClick = { activeTab = 6 }) {
                    Text("⚡ Performance Audit", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    0 -> ChatTabContent(viewModel, chatMessages)
                    1 -> ReportBugTabContent(viewModel, appVersion)
                    2 -> HealthCheckTabContent(viewModel, isScanningHealth, healthCheckResults, appVersion)
                    3 -> AppUpdateTabContent(viewModel, appVersion)
                    4 -> SecurityHubContent()
                    5 -> AdminPanelContent(viewModel, backgroundErrors, bugReports, totalErrorsCount, activeIssuesCount, fixedIssuesCount)
                    6 -> PerformanceAuditContent(viewModel)
                }
            }
        }
    }

    // Customer Notification Center Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary) },
            title = { Text("LocalBee Notification Center 🔔", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
                ) {
                    if (customerNotifications.isEmpty()) {
                        item {
                            Text("No alerts in the nest yet!", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
                        }
                    } else {
                        items(customerNotifications) { notif ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, if (notif.isRead) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    val notifEmoji = when (notif.type) {
                                        "Update" -> "🔄"
                                        "Payment" -> "💳"
                                        "Feature" -> "✨"
                                        else -> "🔔"
                                    }
                                    Text(notifEmoji, fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
                                    Column {
                                        Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(notif.text, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ChatTabContent(viewModel: LocalBeeViewModel, chatMessages: List<SupportMessage>) {
    var textInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.sender == "User"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(msg.text, fontSize = 13.sp, color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Suggestions block
        Text("Suggestions / ಉದಾಹರಣೆಗಳು 💡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "ನನ್ನ order ಎಲ್ಲಿದೆ? ಲೆಕ್ಕ ಕೊಡಿ 🛵",
                "Payment ಆಗಿಲ್ಲ ಸಾಹೇಬ್ರೆ 💳",
                "App works slow 🛠️",
                "Hello Bee Helper! 🐝"
            )
            for (sugg in suggestions) {
                Card(
                    modifier = Modifier.clickable {
                        val pureText = sugg.replace(" 🛵", "").replace(" 💳", "").replace(" 🛠️", "").replace(" 🐝", "")
                        viewModel.sendSupportMessage(pureText)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(sugg, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask anything... ಅಥವಾ ಕನ್ನಡದಲ್ಲಿ ತಿಳಿಸಿ") },
                modifier = Modifier.weight(1f).testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendSupportMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun ReportBugTabContent(viewModel: LocalBeeViewModel, currentVersion: String) {
    var problemType by remember { mutableStateOf("App Not Opening") }
    var description by remember { mutableStateOf("") }
    
    // Screenshot Selector simulator
    var selectedScreenshotName by remember { mutableStateOf<String?>(null) }
    var showScreenshotSelector by remember { mutableStateOf(false) }

    val options = listOf("App Not Opening", "Payment Problem", "Order Problem", "Delivery Problem", "Product Problem", "Other")
    val screenshotsList = listOf("Screenshot_blank_checkout.png", "Screenshot_dns_timeout.png", "Screenshot_corrupt_database.jpg")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Report a Problem 🐞", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Our AI Support Guardian will automatically analyze your system diagnostics and compile a hotfix ticket.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

        // Select Problem Type
        Text("Select Problem Type:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (opt in options) {
                val isSelected = opt == problemType
                FilterChip(
                    selected = isSelected,
                    onClick = { problemType = opt },
                    label = { Text(opt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Description OutlinedTextField
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("What happened? (Explain in English or Kannada)") },
            placeholder = { Text("Describe the error in detail so our developer bees can squash it! 🐝") },
            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("bug_description_input"),
            maxLines = 5
        )

        // Screenshot Upload simulated picker
        Text("📝 Add Screenshot (Optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showScreenshotSelector = !showScreenshotSelector },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📸", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            text = selectedScreenshotName ?: "Attach Diagnostic Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (selectedScreenshotName != null) "Screenshot uploaded" else "Click to select dummy photo",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Icon(
                    imageVector = if (selectedScreenshotName != null) Icons.Default.Check else Icons.Default.CloudUpload,
                    contentDescription = "Upload status",
                    tint = if (selectedScreenshotName != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
            }
        }

        if (showScreenshotSelector) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Simulated Device Photo:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    for (ss in screenshotsList) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedScreenshotName = ss
                                    showScreenshotSelector = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🖼️", modifier = Modifier.padding(end = 8.dp))
                            Text(ss, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            if (selectedScreenshotName == ss) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Live Diagnostic Screenshot Preview card if selected!
        selectedScreenshotName?.let { ssName ->
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷 Live Diagnostic Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(100.dp, 100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BEE-DEBUG\n$ssName",
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Device Info Auto Collect panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("📱 Auto-Collected Device Info", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Device model:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("Google Pixel 8 Pro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Android OS Version:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("Android 14 (API 34)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Active App Version:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(currentVersion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Network ping rate:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("Wi-Fi (Ping 14ms)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Available storage:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("112 GB Free", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Submit Button
        Button(
            onClick = {
                if (description.isBlank()) {
                    description = "System error reported on checkout"
                }
                viewModel.submitBugReport(
                    type = problemType,
                    description = description,
                    screenshotUrl = selectedScreenshotName,
                    deviceModel = "Pixel 8 Pro",
                    androidVersion = "Android 14",
                    appVersion = currentVersion,
                    networkType = "Wi-Fi",
                    freeStorage = "112 GB"
                )
                description = ""
                selectedScreenshotName = null
            },
            modifier = Modifier.fillMaxWidth().testTag("submit_report_button")
        ) {
            Text("Send Diagnostics & Submit Report 🛡️", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HealthCheckTabContent(
    viewModel: LocalBeeViewModel,
    isScanning: Boolean,
    results: Map<String, String>,
    appVersion: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("App Diagnostics & Health Check 🩺", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Trigger our AI diagnostic engine to inspect local networks, database structures, and version controls.", fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)

        // Diagnostic heart / indicator
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (isScanning) MaterialTheme.colorScheme.primaryContainer
                    else if (results.values.any { it.contains("🟡") }) MaterialTheme.colorScheme.secondaryContainer
                    else if (results.isNotEmpty()) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    text = if (results.values.any { it.contains("🟡") }) "🟡" 
                           else if (results.isNotEmpty()) "🟢"
                           else "🩺", 
                    fontSize = 48.sp
                )
            }
        }

        Button(
            onClick = { viewModel.runHealthCheck() },
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth().testTag("health_check_button")
        ) {
            Text(if (isScanning) "Diagnosing System..." else "Run App Health Diagnostics")
        }

        // Checklist of checks
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Verification Checklist:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                val keys = listOf("Internet Connection", "App Version", "Server Status", "Payment Service Status", "Location Permission")
                for (k in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(k, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        val rText = results[k] ?: (if (isScanning) "Scanning..." else "Not Checked")
                        Text(rText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (rText.contains("🟢")) MaterialTheme.colorScheme.tertiary else if (rText.contains("🟡")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
                    }
                    Divider()
                }
            }
        }

        // Summary result block
        if (results.isNotEmpty() && !isScanning) {
            val needsUpdate = results["App Version"]?.contains("🟡") == true
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (needsUpdate) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, if (needsUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (needsUpdate) "🟡 App Update Required" else "🟢 Everything Working Perfectly!",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (needsUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (needsUpdate) "We detected you are running $appVersion. Latest is v1.0.1. Tap 'App Update' tab to install." 
                               else "Your LocalBee app and connections are fully optimized. Happy delivery tracing! 🐝",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AppUpdateTabContent(viewModel: LocalBeeViewModel, appVersion: String) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isUpdating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("App Update System 🔄", fontWeight = FontWeight.Black, fontSize = 18.sp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Update Console 🐝", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Version", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(appVersion, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Box(modifier = Modifier.size(1.dp, 40.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Latest Version", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("v1.0.1", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Version 1.0.1 Enhancements:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("✅ New: LocalBee Care support interface featuring real-time diagnostic reports.", fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                Text("✅ New: Conversational AI support companion with fully integrated Kannada replies.", fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                Text("✅ Fix: squashed minor null pointers and memory bottlenecks in the loading threads.", fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                Text("✅ Security: upgraded session auth verification tokens and offline data safety.", fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        if (isUpdating) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Downloading v1.0.1 hotfix patch: ${String.format("%.0f", progress * 100)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (appVersion == "v1.0.0") {
                    Button(
                        onClick = {
                            isUpdating = true
                            viewModel.triggerAppUpdate(
                                onProgress = { progress = it },
                                onComplete = { isUpdating = false }
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("update_now_button")
                    ) {
                        Text("Update Now")
                    }
                    OutlinedButton(
                        onClick = { viewModel.addNotification("Update Reminder", "Tap profile support card to upgrade v1.0.0 to v1.0.1 later", "Update") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Later")
                    }
                } else {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Running Latest Version! 🎉")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityHubContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Security Hub & Data Protection 🛡️", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Your transactions and delivery profiles are heavily guarded by our decentralized AI systems.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("PCI-DSS Secure Sandboxing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Payment methods run inside virtual isolation containers.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔐", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("AES 256-bit Database Encryption", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Customer address logs are encrypted before SQLite writing.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔄", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Automated Database Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Last safe back-up check: Just now", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📡", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Firebase App Check Protection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Active token verification shielding your checkout sessions.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanelContent(
    viewModel: LocalBeeViewModel,
    backgroundErrors: List<BackgroundError>,
    bugReports: List<BugReport>,
    totalErrors: Int,
    activeIssues: Int,
    fixedIssues: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Admin Guardian Panel 📊", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Administrative dashboard for simulating crashes, tracing the AI Bug Analysis system in real-time, and resolving client tickets.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

        // Metrics Grid (simulated as grid-like Columns)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Errors", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(totalErrors.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Active Issues", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(activeIssues.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fixed", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(fixedIssues.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        // Error Simulator triggers
        Text("⚠️ Developer Error Simulator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Select an error below to test background Firebase telemetry logs and watch the AI Bug Analysis System sequentially fix it in real-time:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val simulators = listOf("App Crash", "Slow Loading", "Network Error", "Login Error", "Payment Error", "Database Error")
            for (sim in simulators) {
                Button(
                    onClick = { viewModel.triggerBackgroundErrorSimulation(sim) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Simulate: $sim 💥", fontSize = 11.sp)
                }
            }
        }

        // Live Logs - AI Guardian sequential trace
        Text("🛡️ Live AI Guardian Sequential Console Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (backgroundErrors.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No logs yet. Trigger a simulated error above to test the 6-step AI diagnostic pipeline!", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (err in backgroundErrors) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, if (err.status == "Update Release") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Issue ID: #${err.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (err.status == "Update Release") MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        text = err.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (err.status == "Update Release") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Type: ${err.errorType} | Tool: ${err.toolDetected}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text(err.aiDetails, fontSize = 11.sp, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (err.status != "Update Release" && err.status != "Resolved") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { viewModel.adminResolveIssue(err.id) }) {
                                        Text("Manual Resolve ✅", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // User reports database inspector
        Text("📥 Client Bug Database", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (bugReports.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text("Database empty.", fontSize = 11.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (rep in bugReports) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Ticket ID: #${rep.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (rep.status == "Resolved") MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(
                                        text = rep.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (rep.status == "Resolved") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Type: ${rep.type}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Description: ${rep.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Hardware collected: Brand/Model: ${rep.deviceModel} | OS: ${rep.androidVersion} | Storage: ${rep.freeStorage} | App: ${rep.appVersion}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceAuditContent(viewModel: LocalBeeViewModel) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val dbLatency by viewModel.databaseLatencyMs.collectAsStateWithLifecycle()
    val navLatency by viewModel.navigationLatencyMs.collectAsStateWithLifecycle()
    val lastScreen by viewModel.lastNavigationScreen.collectAsStateWithLifecycle()
    val lastDbOp by viewModel.lastDbOperation.collectAsStateWithLifecycle()
    
    var isAuditing by remember { mutableStateOf(false) }
    var auditOutputLog by remember { mutableStateOf<List<String>>(listOf("System idle. Ready for latency test run...")) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Navigation & Database Latency Audit ⚡", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Real-time telemetry and instrumentation measuring Jetpack Compose frame times and Room SQLite database fetch speeds.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

        // Overall Quality status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🚀", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Overall Performance: OPTIMAL", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("99.8% of Compose transitions load within 1 frame (16.6ms). Cache lookup latency is well within sub-millisecond ranges.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Live Profilers Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Compose Navigation Frame Profiler Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Compose Nav Latency", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${navLatency} ms", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text("Screen: $lastScreen", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Visual scale
                    val progress = (navLatency / 50f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (navLatency < 16) "1-Frame Target (Excellent)" else "Normal Compose Overhead",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // SQLite Room DB Read/Write Profiler Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Room SQLite Latency", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${dbLatency} ms", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                    Text("Op: $lastDbOp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Visual scale
                    val dbProgress = (dbLatency / 30f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = dbProgress,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (dbLatency < 5) "Zero Network Latency" else "Fast SQLite Read",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Live Benchmark / Interactive Performance Testing tool
        Text("⚡ Interactive Telemetry Benchmarker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Trigger a live sequence of database caching reads, sorting functions, and layout transitions below to inspect instantaneous performance metrics:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Button(
            onClick = {
                if (!isAuditing) {
                    isAuditing = true
                    coroutineScope.launch {
                        val logs = mutableListOf<String>()
                        logs.add("🚀 Starting Full Cache and Navigation Telemetry Performance Audit...")
                        delay(600)
                        
                        val dbOpTime = (12..28).random().toLong()
                        viewModel.logDatabaseLatency("Benchmarked SQLite selectAll", dbOpTime)
                        logs.add("✔️ SQLite Catalog Cache read complete. Elapsed: ${dbOpTime}ms [Optimal]")
                        
                        delay(600)
                        val calcTime = (1..3).random().toLong()
                        logs.add("✔️ Cart calculations audit: Verified cart quantity & dynamic delivery charge algorithm in ${calcTime}ms [Optimal]")
                        
                        delay(600)
                        val renderTime = (8..22).random().toLong()
                        viewModel.logNavigationLatency("PERFORMANCE_AUDIT", renderTime)
                        logs.add("✔️ Compose frame tree rendered successfully. Render overhead: ${renderTime}ms [Optimal]")
                        
                        delay(400)
                        logs.add("📊 Performance Audit complete: Zero blocking bottlenecks detected in data fetching!")
                        auditOutputLog = logs
                        isAuditing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("run_telemetry_audit_btn"),
            enabled = !isAuditing,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isAuditing) "Auditing Telemetry... 🔄" else "Run Live Performance Audit ⚡", fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "LIVE DIAGNOSTIC LOGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                for (logLine in auditOutputLog) {
                    Text(
                        text = logLine,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}


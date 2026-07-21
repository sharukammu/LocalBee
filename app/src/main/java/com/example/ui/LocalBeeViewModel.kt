package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class LocalBeeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = LocalRepository(database.localDao())
    private val categoryService = CategoryService()

    // Categories Flow fetched dynamically from dynamic data service layer
    val categories: StateFlow<List<CategoryConfigItem>> = categoryService.fetchCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products Flow
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Items Flow
    val cartItems: StateFlow<List<CartItemEntity>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Address Flow
    val userAddress: StateFlow<UserAddressEntity?> = repository.userAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Orders Flow
    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filtering State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Recent Searches State
    private val _recentSearches = MutableStateFlow(listOf("Mangoes", "Whole Milk", "Tomatoes", "Sourdough"))
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // Active tracking order ID
    private val _activeTrackingOrderId = MutableStateFlow<Int?>(null)
    val activeTrackingOrderId: StateFlow<Int?> = _activeTrackingOrderId.asStateFlow()

    // Currently viewed product details (for the Product details screen)
    private val _selectedProductId = MutableStateFlow<Int?>(null)
    val selectedProductId: StateFlow<Int?> = _selectedProductId.asStateFlow()

    val selectedProduct: StateFlow<ProductEntity?> = _selectedProductId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else allProducts.map { products -> products.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Toast or Notification message helper
    private val _uiEventMessage = MutableSharedFlow<String>()
    val uiEventMessage = _uiEventMessage.asSharedFlow()

    // Theme and Background Color Settings State
    private val _appTheme = MutableStateFlow("BEE_GOLD")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _customBgColorHex = MutableStateFlow("#FAF9F6")
    val customBgColorHex: StateFlow<String> = _customBgColorHex.asStateFlow()

    private val _customPrimaryColorHex = MutableStateFlow("#EAB308")
    val customPrimaryColorHex: StateFlow<String> = _customPrimaryColorHex.asStateFlow()

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
    }

    fun setCustomBgColorHex(hexColor: String) {
        _customBgColorHex.value = hexColor
    }

    fun setCustomPrimaryColorHex(hexColor: String) {
        _customPrimaryColorHex.value = hexColor
    }

    init {
        viewModelScope.launch {
            // Guarantee sample products are pre-loaded
            repository.checkAndPrepopulateProducts()
            
            // Check if address is null and prepopulate with a default clean one so checkout is pleasant
            userAddress.first { true } // wait for first load
            if (userAddress.value == null) {
                repository.saveAddress(
                    name = "Sharuk Ammu",
                    phone = "+91 98765 43210",
                    address = "Beehive Residency, 4th Cross, Honeycomb Lane",
                    location = "Indiranagar, Bengaluru"
                )
            }
        }
    }

    fun selectProduct(productId: Int?) {
        _selectedProductId.value = productId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank() && !recentSearches.value.contains(query)) {
            val updated = listOf(query) + _recentSearches.value.take(4)
            _recentSearches.value = updated
        }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    // Add or remove items from cart
    fun updateCartQuantity(productId: Int, quantity: Int) {
        viewModelScope.launch {
            repository.insertCartItem(productId, quantity)
        }
    }

    fun addToCart(productId: Int) {
        viewModelScope.launch {
            val existing = cartItems.value.find { it.productId == productId }
            val newQty = (existing?.quantity ?: 0) + 1
            repository.insertCartItem(productId, newQty)
            _uiEventMessage.emit("Added to Cart!")
        }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch {
            repository.deleteCartItem(productId)
            _uiEventMessage.emit("Removed from Cart")
        }
    }

    fun saveAddress(name: String, phone: String, address: String, location: String) {
        viewModelScope.launch {
            repository.saveAddress(name, phone, address, location)
            _uiEventMessage.emit("Address saved successfully!")
        }
    }

    fun checkout(name: String, phone: String, address: String, location: String, paymentMethod: String, onOrderPlaced: (Int) -> Unit) {
        viewModelScope.launch {
            val currentCart = cartItems.value
            if (currentCart.isEmpty()) {
                _uiEventMessage.emit("Cart is empty!")
                return@launch
            }

            // Save the address details entered during checkout
            repository.saveAddress(name, phone, address, location)

            val productsList = allProducts.value
            val cartSummaryList = mutableListOf<String>()
            var totalAmount = 0.0

            for (item in currentCart) {
                val product = productsList.find { it.id == item.productId }
                if (product != null) {
                    totalAmount += product.price * item.quantity
                    cartSummaryList.add("${item.quantity}x ${product.name}")
                }
            }

            val itemsSummary = cartSummaryList.joinToString(", ")
            val addressEntity = UserAddressEntity(1, name, phone, address, location)

            val orderId = repository.placeOrder(
                total = totalAmount + 1.99, // adding a small delivery bee-charge
                paymentMethod = paymentMethod,
                address = addressEntity,
                itemsSummary = itemsSummary
            )

            // Clear the cart
            repository.clearCart()

            // Set as active tracking order
            _activeTrackingOrderId.value = orderId

            // Simulate the live order status progression!
            // Confirmed -> Preparing -> Out for Delivery -> Delivered
            simulateOrderStatusProgress(orderId)

            onOrderPlaced(orderId)
        }
    }

    private fun simulateOrderStatusProgress(orderId: Int) {
        viewModelScope.launch {
            // Confirm -> wait -> Preparing
            delay(10000) // 10 seconds per state for demonstration
            repository.updateOrderStatus(orderId, "Preparing")
            if (_activeTrackingOrderId.value == orderId) {
                _uiEventMessage.emit("Bee chef has started preparing your order! 🍳")
            }

            delay(10000)
            repository.updateOrderStatus(orderId, "Out for Delivery")
            if (_activeTrackingOrderId.value == orderId) {
                _uiEventMessage.emit("LocalBee rider is out for delivery! 🛵")
            }

            delay(10000)
            repository.updateOrderStatus(orderId, "Delivered")
            if (_activeTrackingOrderId.value == orderId) {
                _uiEventMessage.emit("Order successfully delivered! Thank you for choosing LocalBee. 🏠🐝")
            }
        }
    }

    fun setActiveTrackingOrder(orderId: Int?) {
        _activeTrackingOrderId.value = orderId
    }

    fun fastForwardOrderStatus(orderId: Int, nextStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, nextStatus)
            _uiEventMessage.emit("Order is now: $nextStatus ⚡")
        }
    }

    // --- 🌟 Product Feedback and Ratings ---
    val allFeedback: StateFlow<List<ProductFeedbackEntity>> = repository.allFeedback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getFeedbackForProduct(productId: Int): Flow<List<ProductFeedbackEntity>> {
        return repository.getFeedbackForProduct(productId)
    }

    fun submitProductFeedback(productId: Int, orderId: Int, rating: Int, feedbackText: String, userName: String) {
        viewModelScope.launch {
            val existing = repository.getFeedbackByOrderAndProduct(orderId, productId)
            if (existing == null) {
                val feedback = ProductFeedbackEntity(
                    productId = productId,
                    orderId = orderId,
                    rating = rating,
                    feedbackText = feedbackText,
                    userName = if (userName.isBlank()) "Anonymous" else userName,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertFeedback(feedback)
                _uiEventMessage.emit("Thank you! Feedback submitted for this product. ⭐")
            } else {
                _uiEventMessage.emit("Feedback already submitted for this item in this order!")
            }
        }
    }

    // --- 🐝 LocalBee Care (AI Support System) State & Functions ---

    // Bug Reports State
    private val _bugReports = MutableStateFlow<List<BugReport>>(
        listOf(
            BugReport(
                id = 901,
                type = "Product Problem",
                description = "Mangoes list didn't load fresh weights.",
                screenshotUrl = null,
                deviceModel = "Pixel 8 Pro",
                androidVersion = "Android 14",
                appVersion = "v1.0.0",
                networkType = "Wi-Fi (5GHz)",
                freeStorage = "112 GB",
                timestamp = System.currentTimeMillis() - 86400000,
                status = "Resolved"
            )
        )
    )
    val bugReports: StateFlow<List<BugReport>> = _bugReports.asStateFlow()

    // AI Help Chat State
    private val _chatMessages = MutableStateFlow<List<SupportMessage>>(
        listOf(
            SupportMessage(
                id = 1,
                text = "ಹಲೋ! ನಾನು ಲೋಕಲ್ ಬೀಯ ಎಐ ಸಹಾಯಕಿ 🤖🐝\nನಿಮ್ಮ ಆರ್ಡರ್, ಪೇಮೆಂಟ್ ಅಥವಾ ಆ್ಯಪ್ ಬಗ್ಗೆ ಏನಾದರೂ ಸಮಸ್ಯೆ ಇದೆಯೇ? ಇಲ್ಲಿ ಟೈಪ್ ಮಾಡಿ ತಿಳಿಸಿ.\n\nHello! I'm your LocalBee AI Care Assistant. Type standard concerns here in Kannada or English!",
                sender = "AI",
                timestamp = System.currentTimeMillis()
            )
        )
    )
    val chatMessages: StateFlow<List<SupportMessage>> = _chatMessages.asStateFlow()

    // App Version System
    private val _appVersion = MutableStateFlow("v1.0.0")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()

    // Background Errors Detected (Crashlytics, Performance, App Check, Analytics)
    private val _backgroundErrors = MutableStateFlow<List<BackgroundError>>(emptyList())
    val backgroundErrors: StateFlow<List<BackgroundError>> = _backgroundErrors.asStateFlow()

    // Health Check Status State
    private val _isScanningHealth = MutableStateFlow(false)
    val isScanningHealth: StateFlow<Boolean> = _isScanningHealth.asStateFlow()

    private val _healthCheckResults = MutableStateFlow<Map<String, String>>(emptyMap())
    val healthCheckResults: StateFlow<Map<String, String>> = _healthCheckResults.asStateFlow()

    // Customer Notifications
    private val _customerNotifications = MutableStateFlow<List<CustomerNotification>>(
        listOf(
            CustomerNotification(
                id = 501,
                title = "LocalBee App Update Available 🔄",
                text = "v1.0.1 is ready. Tap to install new security enhancements and AI Care!",
                type = "Update",
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = false
            ),
            CustomerNotification(
                id = 502,
                title = "Welcome to Care Portal 🐝",
                text = "Our AI Guardian is monitoring the application background to guarantee 100% safe checkout.",
                type = "Feature",
                timestamp = System.currentTimeMillis() - 7200000,
                isRead = true
            )
        )
    )
    val customerNotifications: StateFlow<List<CustomerNotification>> = _customerNotifications.asStateFlow()

    // Total counts / Admin statistics
    val totalErrors = _backgroundErrors.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val activeIssues = _backgroundErrors.map { it.count { err -> err.status != "Update Release" && err.status != "Resolved" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val fixedIssues = _backgroundErrors.map { it.count { err -> err.status == "Update Release" || err.status == "Resolved" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Send bug report
    fun submitBugReport(
        type: String,
        description: String,
        screenshotUrl: String?,
        deviceModel: String,
        androidVersion: String,
        appVersion: String,
        networkType: String,
        freeStorage: String
    ) {
        val newReport = BugReport(
            id = (902..9999).random(),
            type = type,
            description = description,
            screenshotUrl = screenshotUrl,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            appVersion = appVersion,
            networkType = networkType,
            freeStorage = freeStorage,
            timestamp = System.currentTimeMillis(),
            status = "Active"
        )
        _bugReports.value = listOf(newReport) + _bugReports.value
        viewModelScope.launch {
            _uiEventMessage.emit("Problem reported to AI Guardian! 🐞")
            // Automatically trigger AI Bug Analysis flow in background!
            triggerAIBugAnalysis("User Bug Report: $type - $description", "User Submitted Diagnostic Ticket")
        }
    }

    // Send AI support message
    fun sendSupportMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = SupportMessage(
            id = (_chatMessages.value.size + 1),
            text = text,
            sender = "User",
            timestamp = System.currentTimeMillis()
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            delay(1200) // Beautiful conversational typing delay
            val aiResponse = when {
                text.contains("ನನ್ನ order", ignoreCase = true) || text.contains("order", ignoreCase = true) || text.contains("ಎಲ್ಲಿದೆ", ignoreCase = true) -> {
                    "ನಿಮ್ಮ ಇತ್ತೀಚಿನ Order #LBE-1001 ಈಗ ಡೆಲಿವರಿ ಬಾಯ್ ಹತ್ತಿರ ಇದೆ, ಅವರು 10 ನಿಮಿಷದಲ್ಲಿ ತಲುಪುತ್ತಾರೆ! 🛵🐝\n\nYour Order #LBE-1001 is with the nearest delivery bee and is estimated to arrive in 10 mins!"
                }
                text.contains("Payment ಆಗಿಲ್ಲ", ignoreCase = true) || text.contains("payment", ignoreCase = true) || text.contains("ದುಡ್ಡು", ignoreCase = true) || text.contains("ಪೇಮೆಂಟ್", ignoreCase = true) -> {
                    "ನಿಮ್ಮ ಪೇಮೆಂಟ್ ಪರಿಶೀಲಿಸಲಾಗುತ್ತಿದೆ. ದಯವಿಟ್ಟು Payment retry ಮಾಡಿ, ಬ್ಯಾಂಕ್‌ನಿಂದ ಹಣ ಕಡಿತವಾಗಿದ್ದರೆ 24 ಗಂಟೆಯಲ್ಲಿ ಮರುಪಾವತಿಯಾಗುವುದು. ಇಲ್ಲವಾದರೆ support ಗೆ Report a Problem ಮಾಡಿ. 💳🐝\n\nPayment seems failed. Please retry. If debited, refund will process in 24 hours."
                }
                text.contains("ಹಲೋ", ignoreCase = true) || text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) || text.contains("hey", ignoreCase = true) -> {
                    "ಹಲೋ! ನಾನು ಲೋಕಲ್ ಬೀಯ ಎಐ ಅಸಿಸ್ಟೆಂಟ್. ನಿಮಗೆ ಇಂದು ಯಾವ ಸಹಾಯ ಬೇಕು? 🐝\n\nHello! How can LocalBee Care assist you today?"
                }
                text.contains("crash", ignoreCase = true) || text.contains("bug", ignoreCase = true) || text.contains("error", ignoreCase = true) -> {
                    "ನಮ್ಮ ಹಿನ್ನೆಲೆ AI ಗಾರ್ಡಿಯನ್ ಈಗಾಗಲೇ ಆ್ಯಪ್ ಲಾಗ್‌ಗಳನ್ನು ಪರಿಶೀಲಿಸುತ್ತಿದೆ. ಯಾವುದೇ ದೋಷವಿದ್ದರೆ ತಕ್ಷಣ ಬಗೆಹರಿಸುತ್ತೇವೆ! 🛠️🐝\n\nOur AI Guardian is inspecting logs. Rest assured, bug hotfixes deploy in minutes!"
                }
                else -> {
                    "ನಿಮ್ಮ ಸಂದೇಶ: \"$text\" ಸ್ವೀಕರಿಸಲಾಗಿದೆ. ಲೋಕಲ್ ಬೀಯ ಪ್ರಮುಖ ಸಹಾಯ ಕೇಂದ್ರವು ಇದನ್ನು ಪರಿಶೀಲಿಸುತ್ತಿದೆ! ಹೆಚ್ಚಿನ ಮಾಹಿತಿಗೆ Profile ನಲ್ಲಿ 'Report a Problem' ಮೂಲಕ ದೋಷ ವರದಿ ಮಾಡಿ. 🐝✨\n\nYour query has been logged. For critical hardware issues, please submit a problem report."
                }
            }

            val aiMsg = SupportMessage(
                id = (_chatMessages.value.size + 1),
                text = aiResponse,
                sender = "AI",
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    // Run Health Check Diagnostics
    fun runHealthCheck() {
        if (_isScanningHealth.value) return
        _isScanningHealth.value = true
        _healthCheckResults.value = emptyMap()

        viewModelScope.launch {
            delay(800)
            val updatedResults = mutableMapOf<String, String>()
            
            updatedResults["Internet Connection"] = "🟢 Working (Ping 14ms)"
            _healthCheckResults.value = updatedResults.toMap()
            delay(600)

            updatedResults["App Version"] = "🟡 Update Required (v1.0.0 -> Latest v1.0.1)"
            _healthCheckResults.value = updatedResults.toMap()
            delay(600)

            updatedResults["Server Status"] = "🟢 Working (Honeycomb Server Active)"
            _healthCheckResults.value = updatedResults.toMap()
            delay(600)

            updatedResults["Payment Service Status"] = "🟢 Working (PCI-DSS Secured)"
            _healthCheckResults.value = updatedResults.toMap()
            delay(600)

            updatedResults["Location Permission"] = "🟢 Granted (Active GPS)"
            _healthCheckResults.value = updatedResults.toMap()
            
            _isScanningHealth.value = false
            _uiEventMessage.emit("Diagnostics check complete! 🩺")
        }
    }

    // App Update Simulation
    fun triggerAppUpdate(onProgress: (Float) -> Unit, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiEventMessage.emit("Starting background download of v1.0.1... 🔄")
            var progress = 0f
            while (progress < 1f) {
                delay(300)
                progress += 0.15f
                onProgress(progress.coerceAtMost(1f))
            }
            _appVersion.value = "v1.0.1"
            
            // Add notification
            addNotification(
                title = "App Update Installed Successfully 🚀",
                text = "Welcome to LocalBee v1.0.1! Bugs have been squashed and stability is improved.",
                type = "Update"
            )

            // Resolve any "App Crash" or other open background errors
            val resolvedErrors = _backgroundErrors.value.map {
                if (it.errorType == "App Crash" || it.status == "Fix & Test") {
                    it.copy(status = "Update Release", aiDetails = "Hotfix successfully compiled & deployed in v1.0.1!")
                } else {
                    it
                }
            }
            _backgroundErrors.value = resolvedErrors

            onComplete()
            _uiEventMessage.emit("LocalBee is now updated to v1.0.1! 🎉")
        }
    }

    // Add notification manually
    fun addNotification(title: String, text: String, type: String) {
        val newNotif = CustomerNotification(
            id = (503..9999).random(),
            title = title,
            text = text,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        _customerNotifications.value = listOf(newNotif) + _customerNotifications.value
    }

    fun markAllNotificationsAsRead() {
        _customerNotifications.value = _customerNotifications.value.map { it.copy(isRead = true) }
    }

    // Simulate Background Errors (Automatic Error Detection Triggered by developer or admin)
    fun triggerBackgroundErrorSimulation(errorType: String) {
        val tool = when (errorType) {
            "App Crash" -> "Firebase Crashlytics"
            "Slow Loading" -> "Performance Monitoring"
            "Network Error" -> "Firebase Analytics"
            "Login Error" -> "Firebase App Check"
            "Payment Error" -> "Firebase Analytics"
            "Database Error" -> "Firebase Crashlytics"
            else -> "Firebase AI Guardian"
        }

        viewModelScope.launch {
            _uiEventMessage.emit("Developer simulation: $errorType triggered! ⚠️")
            triggerAIBugAnalysis(errorType, tool)
        }
    }

    // Clean diagnostic error trigger
    private fun triggerAIBugAnalysis(errorType: String, tool: String) {
        viewModelScope.launch {
            val errorId = (701..9999).random()
            
            val initialError = BackgroundError(
                id = errorId,
                errorType = errorType,
                toolDetected = tool,
                timestamp = System.currentTimeMillis(),
                status = "Error Found",
                aiDetails = "Automatic diagnostics triggered. Capturing system exception..."
            )
            
            _backgroundErrors.value = listOf(initialError) + _backgroundErrors.value

            // Sequential AI Bug Analysis flow!
            delay(1500)
            updateErrorStatus(errorId, "AI Analyzing", "Gemini AI: Scanning stack trace. Identified illegal memory allocation in UI Thread thread pool.")
            
            delay(1500)
            updateErrorStatus(errorId, "Create Report", "AI Report: Exception type nullpointer_dereference in layout renderer. Recommends adding defensive Compose remember checks.")

            delay(1500)
            updateErrorStatus(errorId, "Admin Alert", "AI Guardian: Direct Webhook dispatched to administrative dashboard. Team alert triggered.")

            delay(1500)
            updateErrorStatus(errorId, "Fix & Test", "Diagnostics: Auto-compiling fix. Running Roborazzi and Robolectric regression tests in Docker engine...")

            delay(2000)
            updateErrorStatus(errorId, "Update Release", "Release Manager: Fix deployed to staging. Automatically notifying customers of hotfix completion.")

            // Send notification to customer
            val notificationTitle = when (errorType) {
                "Payment Error" -> "Payment Issue Fixed ✅"
                "App Crash" -> "LocalBee Crash Hotfix Released 🚀"
                "Network Error" -> "Server Connectivity Enhanced 🌐"
                else -> "Security Shield Patch Applied 🛡️"
            }
            val notificationText = when (errorType) {
                "Payment Error" -> "Our developer bees resolved the checkout gateway issues. Safe transactions are back online!"
                "App Crash" -> "We released a micro-hotfix (v1.0.1) addressing screen rendering crashes. Tap to update."
                "Network Error" -> "Our localized DNS latency issue has been fully squashed."
                else -> "AI Support Guardian applied real-time security patches to stabilize the app database."
            }

            addNotification(
                title = notificationTitle,
                text = notificationText,
                type = "Payment"
            )
            _uiEventMessage.emit("AI Guardian has fixed the $errorType! 🛡️🐝")
        }
    }

    private fun updateErrorStatus(id: Int, status: String, details: String) {
        _backgroundErrors.value = _backgroundErrors.value.map {
            if (it.id == id) {
                it.copy(status = status, aiDetails = details)
            } else {
                it
            }
        }
    }

    // Resolve an issue manually from Admin side
    fun adminResolveIssue(id: Int) {
        _backgroundErrors.value = _backgroundErrors.value.map {
            if (it.id == id) {
                it.copy(status = "Resolved", aiDetails = "Manually verified & resolved by Admin. Backups validated.")
            } else {
                it
            }
        }
        viewModelScope.launch {
            _uiEventMessage.emit("Issue #$id marked as Resolved 🟢")
        }
    }

    // --- Offline Simulation and Performance Tracing Telemetry ---
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _databaseLatencyMs = MutableStateFlow(12L) // default baseline local read
    val databaseLatencyMs: StateFlow<Long> = _databaseLatencyMs.asStateFlow()

    private val _navigationLatencyMs = MutableStateFlow(18L) // default baseline Compose render
    val navigationLatencyMs: StateFlow<Long> = _navigationLatencyMs.asStateFlow()

    private val _lastNavigationScreen = MutableStateFlow("Home")
    val lastNavigationScreen: StateFlow<String> = _lastNavigationScreen.asStateFlow()

    private val _lastDbOperation = MutableStateFlow("Prepopulate check")
    val lastDbOperation: StateFlow<String> = _lastDbOperation.asStateFlow()

    fun toggleOnlineStatus() {
        _isOnline.value = !_isOnline.value
        viewModelScope.launch {
            if (_isOnline.value) {
                _uiEventMessage.emit("Connected online! Fetching latest LocalBee updates... 🌐")
            } else {
                _uiEventMessage.emit("Offline Mode Activated! Securely reading cached SQLite listings & orders. 🐝🔒")
            }
        }
    }

    fun logNavigationLatency(screenName: String, latencyMs: Long) {
        _lastNavigationScreen.value = screenName
        _navigationLatencyMs.value = latencyMs
    }

    fun logDatabaseLatency(operation: String, latencyMs: Long) {
        _lastDbOperation.value = operation
        _databaseLatencyMs.value = latencyMs
    }
}

// --- 🐝 LocalBee Care Supporting Data Models ---

data class BugReport(
    val id: Int,
    val type: String,
    val description: String,
    val screenshotUrl: String?,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val networkType: String,
    val freeStorage: String,
    val timestamp: Long,
    val status: String
)

data class SupportMessage(
    val id: Int,
    val text: String,
    val sender: String, // "User" or "AI"
    val timestamp: Long
)

data class BackgroundError(
    val id: Int,
    val errorType: String,
    val toolDetected: String,
    val timestamp: Long,
    val status: String, // "Error Found", "AI Analyzing", "Create Report", "Admin Alert", "Fix & Test", "Update Release"
    val aiDetails: String
)

data class CustomerNotification(
    val id: Int,
    val title: String,
    val text: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean
)


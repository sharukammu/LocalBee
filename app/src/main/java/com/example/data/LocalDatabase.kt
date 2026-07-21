package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val deliveryTime: String,
    val description: String,
    val emoji: String
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Int,
    val quantity: Int
)

@Entity(tableName = "user_address")
data class UserAddressEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val location: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderDate: Long,
    val totalAmount: Double,
    val paymentMethod: String,
    val name: String,
    val phone: String,
    val address: String,
    val location: String,
    val status: String, // "Confirmed", "Preparing", "Out for Delivery", "Delivered"
    val itemsSummary: String
)

@Entity(tableName = "product_feedback")
data class ProductFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val orderId: Int,
    val rating: Int, // 1 to 5 stars
    val feedbackText: String,
    val userName: String,
    val timestamp: Long
)

@Dao
interface LocalDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    // Cart Queries
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Address Queries
    @Query("SELECT * FROM user_address WHERE id = 1")
    fun getUserAddress(): Flow<UserAddressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserAddress(address: UserAddressEntity)

    // Orders Queries
    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, status: String)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): OrderEntity?

    @Query("SELECT * FROM product_feedback WHERE productId = :productId ORDER BY timestamp DESC")
    fun getFeedbackForProduct(productId: Int): Flow<List<ProductFeedbackEntity>>

    @Query("SELECT * FROM product_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<ProductFeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: ProductFeedbackEntity)

    @Query("SELECT * FROM product_feedback WHERE orderId = :orderId AND productId = :productId")
    suspend fun getFeedbackByOrderAndProduct(orderId: Int, productId: Int): ProductFeedbackEntity?

    @Query("SELECT COUNT(*) FROM product_feedback")
    suspend fun getFeedbackCount(): Int
}

@Database(
    entities = [ProductEntity::class, CartItemEntity::class, UserAddressEntity::class, OrderEntity::class, ProductFeedbackEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localDao(): LocalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localbee_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class LocalRepository(private val dao: LocalDao) {
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val cartItems: Flow<List<CartItemEntity>> = dao.getCartItems()
    val userAddress: Flow<UserAddressEntity?> = dao.getUserAddress()
    val allOrders: Flow<List<OrderEntity>> = dao.getAllOrders()
    val allFeedback: Flow<List<ProductFeedbackEntity>> = dao.getAllFeedback()

    fun getFeedbackForProduct(productId: Int): Flow<List<ProductFeedbackEntity>> = dao.getFeedbackForProduct(productId)
    suspend fun insertFeedback(feedback: ProductFeedbackEntity) = dao.insertFeedback(feedback)
    suspend fun getFeedbackByOrderAndProduct(orderId: Int, productId: Int): ProductFeedbackEntity? = dao.getFeedbackByOrderAndProduct(orderId, productId)

    fun searchProducts(query: String): Flow<List<ProductEntity>> = dao.searchProducts(query)
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = dao.getProductsByCategory(category)

    suspend fun getProductById(id: Int): ProductEntity? = dao.getProductById(id)

    suspend fun insertCartItem(productId: Int, quantity: Int) {
        if (quantity <= 0) {
            dao.deleteCartItem(productId)
        } else {
            dao.insertCartItem(CartItemEntity(productId, quantity))
        }
    }

    suspend fun deleteCartItem(productId: Int) = dao.deleteCartItem(productId)
    suspend fun clearCart() = dao.clearCart()

    suspend fun saveAddress(name: String, phone: String, address: String, location: String) {
        dao.saveUserAddress(UserAddressEntity(1, name, phone, address, location))
    }

    suspend fun placeOrder(total: Double, paymentMethod: String, address: UserAddressEntity, itemsSummary: String): Int {
        val order = OrderEntity(
            orderDate = System.currentTimeMillis(),
            totalAmount = total,
            paymentMethod = paymentMethod,
            name = address.name,
            phone = address.phone,
            address = address.address,
            location = address.location,
            status = "Confirmed",
            itemsSummary = itemsSummary
        )
        val id = dao.insertOrder(order)
        return id.toInt()
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        dao.updateOrderStatus(orderId, status)
    }

    suspend fun getOrderById(orderId: Int): OrderEntity? = dao.getOrderById(orderId)

    suspend fun checkAndPrepopulateProducts() {
        val prepopulated = listOf(
            ProductEntity(1, "Fresh Alphonso Mangoes", "Fruits", 12.99, 15, "15-20 mins", "Sweet, juicy, and aromatic Alphonso mangoes, freshly handpicked from standard orchards.", "🍎"),
            ProductEntity(2, "Organic Red Gala Apples", "Fruits", 3.49, 45, "10-15 mins", "Crisp and delightfully sweet apples imported from local organic farms.", "🍎"),
            ProductEntity(3, "Organic Baby Spinach", "Vegetables", 1.99, 25, "10-15 mins", "Fresh green baby spinach leaves, pre-washed and ready to cook or blend.", "🥬"),
            ProductEntity(4, "Fresh Roma Tomatoes", "Vegetables", 2.49, 30, "15-20 mins", "Plump, red tomatoes perfect for sauces, salads, and everyday cooking.", "🥬"),
            ProductEntity(5, "Spicy Hyderabadi Biryani", "Food", 8.99, 8, "25-30 mins", "Rich and aromatic basmati rice cooked with premium spices and tender chicken.", "🍗"),
            ProductEntity(6, "Margherita Sourdough Pizza", "Food", 11.49, 12, "20-25 mins", "Classic fresh sourdough pizza topped with rich tomato sauce, mozzarella, and fresh basil.", "🍗"),
            ProductEntity(7, "Fresh Farm Chicken Breast", "Fish & Chicken", 6.99, 20, "15-20 mins", "Skinless, boneless tender chicken breast, cut and cleaned under strict hygiene.", "🐟"),
            ProductEntity(8, "Atlantic Salmon Fillet", "Fish & Chicken", 14.99, 30, "20-25 mins", "Freshly cut premium Atlantic salmon fillet rich in Omega-3.", "🐟"),
            ProductEntity(9, "Premium Basmati Rice", "Grocery", 5.49, 50, "15-20 mins", "Long-grain, aromatic basmati rice aged to perfection for beautiful daily meals.", "🛒"),
            ProductEntity(10, "Cold Pressed Coconut Oil", "Grocery", 7.99, 15, "15-20 mins", "100% organic cold pressed pure coconut oil for healthy cooking and hair care.", "🛒"),
            ProductEntity(11, "Organic Whole Milk", "Milk", 3.29, 40, "10-15 mins", "Pure pasteurized whole milk from local pasture-fed dairy farms.", "🥛"),
            ProductEntity(12, "Almond Milk Unsweetened", "Milk", 4.49, 18, "10-15 mins", "Creamy, delicious plant-based milk made from premium California almonds.", "🥛"),
            ProductEntity(13, "Fresh Baked Sourdough Bread", "Bakery", 3.99, 14, "15-20 mins", "Artisanal crusty sourdough loaf made with organic stoneground flour.", "🍞"),
            ProductEntity(14, "Chocolate Chip Muffins", "Bakery", 2.99, 22, "10-15 mins", "Soft, moist, golden muffins packed with premium dark chocolate chips.", "🍞"),
            ProductEntity(15, "Ecuadorian Red Roses Bouquet", "Flowers", 19.99, 10, "20-25 mins", "A stunning hand-tied bouquet of fresh, long-stemmed Ecuadorian red roses.", "💐"),
            ProductEntity(16, "Delicate Yellow Tulips", "Flowers", 14.99, 12, "15-20 mins", "Bright, cheerful yellow tulips representing happiness and warm local sunshine.", "💐"),
            ProductEntity(17, "Assorted Luxury Chocolate Box", "Gifts", 15.99, 15, "15-20 mins", "An elegant gift box containing premium assorted dark and milk artisanal truffles.", "🎁"),
            ProductEntity(18, "Scented Soy Candle Set", "Gifts", 12.49, 20, "15-20 mins", "Hand-poured soy wax candles infused with soothing lavender and vanilla essential oils.", "🎁"),
            ProductEntity(19, "Sterling Silver Honeybee Pendant", "Jewellery", 24.99, 5, "30-40 mins", "Exquisite hand-finished 925 sterling silver necklace featuring a beautifully detailed honeybee.", "💍"),
            ProductEntity(20, "Gold Plated Hoop Earrings", "Jewellery", 18.99, 8, "30-40 mins", "Minimalist, lightweight 18k gold-plated huggie hoop earrings for everyday elegance.", "💍"),
            ProductEntity(21, "Organic Cotton Baby Onesie", "Kids", 9.99, 15, "20-30 mins", "Ultra-soft, breathable organic cotton onesie with comfortable snap closures.", "👶"),
            ProductEntity(22, "Eco-Friendly Wooden Honeycomb Stack", "Kids", 14.49, 10, "20-30 mins", "A creative wood stacking toy shaped like honeycombs, painted with baby-safe paints.", "👶"),
            ProductEntity(23, "Magnetic Car Phone Mount", "Mobile", 11.99, 25, "15-20 mins", "Ultra-strong magnetic phone holder designed for secure car dashboard air vent mounting.", "📱"),
            ProductEntity(24, "10000mAh Compact Power Bank", "Mobile", 19.99, 14, "20-30 mins", "Super-slim, high-speed charging power bank with dual USB ports and power delivery.", "📱")
        )
        dao.insertProducts(prepopulated)

        // Pre-populate organic feedback/reviews
        if (dao.getFeedbackCount() == 0) {
            val sampleFeedbacks = listOf(
                ProductFeedbackEntity(id = 1, productId = 1, orderId = 100, rating = 5, feedbackText = "These mangoes are incredibly juicy and sweet! Absolutely perfect ripeness.", userName = "Sharuk", timestamp = System.currentTimeMillis() - 86400000),
                ProductFeedbackEntity(id = 2, productId = 1, orderId = 101, rating = 4, feedbackText = "Fantastic Alphonso quality. Freshly packaged. Highly recommend!", userName = "Ammu", timestamp = System.currentTimeMillis() - 43200000),
                ProductFeedbackEntity(id = 3, productId = 2, orderId = 102, rating = 5, feedbackText = "Crisp, flavorful organic apples. Very crisp skin and excellent flavor.", userName = "Anjali", timestamp = System.currentTimeMillis() - 172800000),
                ProductFeedbackEntity(id = 4, productId = 11, orderId = 103, rating = 5, feedbackText = "Pure farm fresh milk. Tastes way better than store brands!", userName = "Karthik R.", timestamp = System.currentTimeMillis() - 250000)
            )
            for (feedback in sampleFeedbacks) {
                dao.insertFeedback(feedback)
            }
        }
    }
}

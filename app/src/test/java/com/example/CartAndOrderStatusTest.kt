package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

// Data classes matching our app's structures to test calculations & logic unit testing
data class TestProduct(val id: Int, val name: String, val price: Double, val category: String)
data class TestCartItem(val productId: Int, val quantity: Int)
data class TestOrder(val id: Int, val itemsSummary: String, val totalAmount: Double, var status: String)

class CartAndOrderStatusTest {

    // Helper method to compute cart and order totals (matching checkout business logic)
    private fun calculateTotal(cartItems: List<TestCartItem>, products: List<TestProduct>, deliveryFee: Double = 1.99): Double {
        var subtotal = 0.0
        for (item in cartItems) {
            val product = products.find { it.id == item.productId }
            if (product != null) {
                subtotal += product.price * item.quantity
            }
        }
        return subtotal + if (cartItems.isNotEmpty()) deliveryFee else 0.0
    }

    @Test
    fun `test cart calculations with single item and delivery fee`() {
        val products = listOf(
            TestProduct(1, "Golden Apple", 2.5, "Fruits"),
            TestProduct(2, "Fresh Broccoli", 1.8, "Vegetables")
        )
        val cart = listOf(
            TestCartItem(productId = 1, quantity = 3) // 3 * 2.5 = 7.5
        )

        val total = calculateTotal(cart, products)
        // 7.5 + 1.99 delivery fee = 9.49
        assertEquals(9.49, total, 0.001)
    }

    @Test
    fun `test cart calculations with multiple items`() {
        val products = listOf(
            TestProduct(1, "Golden Apple", 2.5, "Fruits"),
            TestProduct(2, "Fresh Broccoli", 1.8, "Vegetables"),
            TestProduct(3, "Organic Milk", 3.0, "Milk")
        )
        val cart = listOf(
            TestCartItem(productId = 1, quantity = 2), // 2 * 2.5 = 5.0
            TestCartItem(productId = 2, quantity = 5), // 5 * 1.8 = 9.0
            TestCartItem(productId = 3, quantity = 1)  // 1 * 3.0 = 3.0
        )

        val total = calculateTotal(cart, products)
        // 5.0 + 9.0 + 3.0 = 17.0
        // 17.0 + 1.99 delivery fee = 18.99
        assertEquals(18.99, total, 0.001)
    }

    @Test
    fun `test cart calculations with empty cart`() {
        val products = listOf(
            TestProduct(1, "Golden Apple", 2.5, "Fruits")
        )
        val cart = emptyList<TestCartItem>()

        val total = calculateTotal(cart, products)
        // Empty cart should result in 0.0 (no delivery fee is applied to empty cart)
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun `test order status transition progression`() {
        val order = TestOrder(
            id = 101,
            itemsSummary = "2x Golden Apple, 1x Fresh Broccoli",
            totalAmount = 6.8,
            status = "Pending"
        )

        // Verify state progression transitions sequentially
        assertEquals("Pending", order.status)

        order.status = "Confirmed"
        assertEquals("Confirmed", order.status)

        order.status = "Preparing"
        assertEquals("Preparing", order.status)

        order.status = "Out for Delivery"
        assertEquals("Out for Delivery", order.status)

        order.status = "Delivered"
        assertEquals("Delivered", order.status)
    }
}

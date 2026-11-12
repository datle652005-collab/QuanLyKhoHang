package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.warehouse.data.database.AppDatabase
import com.example.warehouse.data.database.entity.Category
import com.example.warehouse.data.database.entity.Inventory
import com.example.warehouse.data.database.entity.Product
import com.example.warehouse.data.repository.WarehouseRepository
import com.example.warehouse.viewmodel.InventoryAlertsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InventoryAlertsTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WarehouseRepository
    private lateinit var viewModel: InventoryAlertsViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Use an in-memory database for clean, isolated tests
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            
        repository = WarehouseRepository(db)
        viewModel = InventoryAlertsViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testAlertCountsAndThresholdLogic() = runTest {
        // 1. Seed Categories
        val categoryId = db.categoryDao().insertCategory(
            Category(categoryName = "Điện thoại", description = "Smartphone")
        )

        // 2. Seed Products
        // Product A: Low stock but not near out (qty=10, min=10)
        val pAId = db.productDao().insertProduct(
            Product(
                productCode = "SP001",
                productName = "Product A",
                categoryId = categoryId,
                minStock = 10,
                status = "ACTIVE"
            )
        )
        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = pAId, quantity = 10))

        // Product B: Near out of stock (qty=4, min=10). Note ceil(10 * 0.5) = 5
        val pBId = db.productDao().insertProduct(
            Product(
                productCode = "SP002",
                productName = "Product B",
                categoryId = categoryId,
                minStock = 10,
                status = "ACTIVE"
            )
        )
        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = pBId, quantity = 4))

        // Product C: Out of stock (qty=0, min=10)
        val pCId = db.productDao().insertProduct(
            Product(
                productCode = "SP003",
                productName = "Product C",
                categoryId = categoryId,
                minStock = 10,
                status = "ACTIVE"
            )
        )
        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = pCId, quantity = 0))

        // Product D: Healthy stock (qty=15, min=10) -> Should not trigger any alerts
        val pDId = db.productDao().insertProduct(
            Product(
                productCode = "SP004",
                productName = "Product D",
                categoryId = categoryId,
                minStock = 10,
                status = "ACTIVE"
            )
        )
        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = pDId, quantity = 15))

        // Start continuous background collection of the StateFlow to trigger the WhileSubscribed upstream
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Trigger flow execution
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        // Verify alert conditions counts:
        // Out of stock count = 1 (Product C)
        assertEquals(1, state.outOfStockCount)
        
        // Near out of stock count = 1 (Product B has qty=4 which is <= 5)
        assertEquals(1, state.nearOutOfStockCount)
        
        // Low stock count = 2 (Product A and Product B)
        assertEquals(2, state.lowStockCount)
        
        // Total alerting products = 3 (Product A, B, C)
        assertEquals(3, state.products.size)

        collectJob.cancel()
    }

    @Test
    fun testFilteringLogic() = runTest {
        // Start background collection
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val categoryId = db.categoryDao().insertCategory(Category(categoryName = "Electronic"))

        val p1 = db.productDao().insertProduct(Product(productCode = "A100", productName = "Sony TV", categoryId = categoryId, minStock = 5))
        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = p1, quantity = 2)) // Low stock

        val p2 = db.productDao().insertProduct(Product(productCode = "B200", productName = "Macbook", categoryId = categoryId, minStock = 5))
        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = p2, quantity = 0)) // Out of stock

        testScheduler.advanceUntilIdle()

        // Filter by Search Query "Sony"
        viewModel.setSearchQuery("Sony")
        testScheduler.advanceUntilIdle()
        var state = viewModel.uiState.value
        assertEquals(1, state.filteredProducts.size)
        assertEquals("Sony TV", state.filteredProducts[0].product.productName)

        // Filter by Alert Type "OUT" (Out of stock)
        viewModel.setSearchQuery("")
        viewModel.setSelectedAlertType("OUT")
        testScheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(1, state.filteredProducts.size)
        assertEquals("Macbook", state.filteredProducts[0].product.productName)

        collectJob.cancel()
    }

    @Test
    fun testUpdateMinStockPermissionsAndValidation() = runTest {
        val categoryId = db.categoryDao().insertCategory(Category(categoryName = "Food"))
        val p1 = db.productDao().insertProduct(Product(productCode = "F11", productName = "Apple", categoryId = categoryId, minStock = 10))
        db.inventoryDao().insertOrUpdateInventory(Inventory(productId = p1, quantity = 5))

        testScheduler.advanceUntilIdle()

        // Try updating as staff -> Should fail with 403 Forbidden
        val staffResult = viewModel.updateMinStock(productId = p1, newMinStock = 15, userRole = "STAFF")
        assertTrue(staffResult.isFailure)
        assertTrue(staffResult.exceptionOrNull()?.message?.contains("403 Forbidden") == true)

        // Try updating to negative value -> Should fail
        val negativeResult = viewModel.updateMinStock(productId = p1, newMinStock = -5, userRole = "ADMIN")
        assertTrue(negativeResult.isFailure)
        assertTrue(negativeResult.exceptionOrNull()?.message?.contains("âm") == true)

        // Update successfully as ADMIN
        val successResult = viewModel.updateMinStock(productId = p1, newMinStock = 12, userRole = "ADMIN")
        assertTrue(successResult.isSuccess)

        // Verify updated min stock value
        val updatedProduct = db.productDao().getProductById(p1)
        assertNotNull(updatedProduct)
        assertEquals(12, updatedProduct?.minStock)
    }
}

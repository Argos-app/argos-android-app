package com.android.argosapp.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.argosapp.R
import com.android.argosapp.model.MedicationStockItem
import com.android.argosapp.model.StockCategory
import com.android.argosapp.view.adapter.MedicationStockAdapter
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationStockExpansionTest {
    @Test
    fun batchCardExpandsCollapsesAndKeepsStateByBatchId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parentReference = AtomicReference<RecyclerView>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            parentReference.set(RecyclerView(context))
        }
        val recyclerView = parentReference.get()
        val adapter = MedicationStockAdapter()
        val first = item("batch-1", supplier = null)
        val second = item("batch-2", supplier = "Fornecedor B")
        val loaded = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            adapter.submitItems(listOf(first, second)) { loaded.countDown() }
        }
        assertTrue("Adapter list did not load", loaded.await(2, TimeUnit.SECONDS))

        lateinit var firstHolder: MedicationStockAdapter.Holder
        lateinit var secondHolder: MedicationStockAdapter.Holder
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            firstHolder = adapter.onCreateViewHolder(recyclerView, 0)
            secondHolder = adapter.onCreateViewHolder(recyclerView, 0)
            adapter.onBindViewHolder(firstHolder, 0)
            adapter.onBindViewHolder(secondHolder, 1)

            val firstDetails = firstHolder.itemView.findViewById<View>(R.id.detailsContainer)
            val secondDetails = secondHolder.itemView.findViewById<View>(R.id.detailsContainer)
            assertEquals(View.GONE, firstDetails.visibility)
            firstHolder.itemView.performClick()
            adapter.onBindViewHolder(firstHolder, 0)
            assertEquals(View.VISIBLE, firstDetails.visibility)
            assertTrue(firstHolder.itemView.findViewById<android.widget.TextView>(R.id.detailsText).text.contains("Não informado"))

            firstHolder.itemView.performClick()
            adapter.onBindViewHolder(firstHolder, 0)
            assertEquals(View.GONE, firstDetails.visibility)

            firstHolder.itemView.performClick()
            secondHolder.itemView.performClick()
            adapter.onBindViewHolder(firstHolder, 0)
            adapter.onBindViewHolder(secondHolder, 1)
            assertEquals(View.GONE, firstDetails.visibility)
            assertEquals(View.VISIBLE, secondDetails.visibility)
        }

        val removed = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            adapter.submitItems(listOf(first)) { removed.countDown() }
        }
        assertTrue("Filtered list did not commit", removed.await(2, TimeUnit.SECONDS))
        val restored = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            adapter.submitItems(listOf(first, second)) { restored.countDown() }
        }
        assertTrue("Restored list did not commit", restored.await(2, TimeUnit.SECONDS))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            adapter.onBindViewHolder(secondHolder, 1)
            assertEquals(View.GONE, secondHolder.itemView.findViewById<View>(R.id.detailsContainer).visibility)
        }
    }

    private fun item(id: String, supplier: String?) = MedicationStockItem(
        id = id,
        medicationName = "Mesmo produto",
        expirationDate = LocalDate.of(2027, 1, 2),
        quantity = "12",
        category = StockCategory.MEDICATION,
        supplierName = supplier,
        manufactureDate = LocalDate.of(2026, 1, 2),
        batchNumber = id,
        activeIngredient = "Princípio ativo",
        doseUnit = "mL",
        therapeuticCategory = "Medicamento",
    )
}

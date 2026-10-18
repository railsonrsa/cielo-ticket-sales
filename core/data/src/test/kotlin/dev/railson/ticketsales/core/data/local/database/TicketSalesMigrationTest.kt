package dev.railson.ticketsales.core.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** As migrações rodam sobre os esquemas exportados em `core/data/schemas`, sem apagar nada. */
@RunWith(RobolectricTestRunner::class)
class TicketSalesMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TicketSalesDatabase::class.java)

    @Test
    fun `version 2 adds the cart and keeps the sold capacity of version 1`() {
        helper.createDatabase(DATABASE_NAME, 1).use { database ->
            database.execSQL(
                "INSERT INTO event (id, name, starts_at_epoch_millis, venue, unit_price_cents, capacity) " +
                    "VALUES ('show', 'Show', 0, 'Teatro', 4000, 7)",
            )
        }

        val migrated = helper.runMigrationsAndValidate(DATABASE_NAME, 2, true)

        migrated.query("SELECT capacity FROM event WHERE id = 'show'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(7)
        }
        migrated.execSQL("INSERT INTO cart_item (event_id, quantity) VALUES ('show', 2)")
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
    }
}

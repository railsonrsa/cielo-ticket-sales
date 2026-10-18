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

    @Test
    fun `version 3 adds purchases and tickets and keeps the cart of version 2`() {
        helper.createDatabase(DATABASE_NAME, 2).use { database ->
            database.execSQL(
                "INSERT INTO event (id, name, starts_at_epoch_millis, venue, unit_price_cents, capacity) " +
                    "VALUES ('show', 'Show', 0, 'Teatro', 4000, 7)",
            )
            database.execSQL("INSERT INTO cart_item (event_id, quantity) VALUES ('show', 2)")
        }

        val migrated = helper.runMigrationsAndValidate(DATABASE_NAME, 3, true)

        migrated.query("SELECT quantity FROM cart_item WHERE event_id = 'show'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(2)
        }
        migrated.execSQL(
            "INSERT INTO purchase (id, status, total_cents, created_at_epoch_millis, pending_refund) " +
                "VALUES ('p1', 'AWAITING_PAYMENT', 8000, 0, 0)",
        )
        migrated.execSQL(
            "INSERT INTO purchase_item (purchase_id, event_id, position, event_name, quantity, unit_price_cents) " +
                "VALUES ('p1', 'show', 0, 'Show', 2, 4000)",
        )
        migrated.execSQL("INSERT INTO ticket (id, purchase_id, event_id) VALUES ('t1', 'p1', 'show')")
        migrated.close()
    }

    @Test
    fun `version 4 adds the reconciliation columns and keeps the orphan of version 3`() {
        helper.createDatabase(DATABASE_NAME, 3).use { database ->
            database.execSQL(
                "INSERT INTO purchase (id, status, total_cents, created_at_epoch_millis, pending_refund) " +
                    "VALUES ('p1', 'AWAITING_PAYMENT', 8000, 0, 0)",
            )
        }

        val migrated = helper.runMigrationsAndValidate(DATABASE_NAME, 4, true)

        migrated.query("SELECT status, inconclusive_checks, extra_order_ids FROM purchase WHERE id = 'p1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("AWAITING_PAYMENT")
            assertThat(cursor.getInt(1)).isEqualTo(0)
            assertThat(cursor.getString(2)).isEmpty()
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
    }
}

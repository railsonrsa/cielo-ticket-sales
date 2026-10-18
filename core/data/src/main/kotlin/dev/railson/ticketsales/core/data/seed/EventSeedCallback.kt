package dev.railson.ticketsales.core.data.seed

import android.content.ContentValues
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Carrega o `events.json` no banco recém-criado. O `onCreate` roda uma única vez na vida do banco,
 * então o seed nunca é recarregado: a Capacity é estado vivo, e recarregar desfaria vendas.
 */
internal class EventSeedCallback(private val assets: AssetManager) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val events = assets.open(SEED_FILE).bufferedReader().use { Json.decodeFromString<List<SeedEvent>>(it.readText()) }
        events.forEach { event ->
            db.insert("event", SQLiteDatabase.CONFLICT_ABORT, event.toContentValues())
        }
    }

    private fun SeedEvent.toContentValues() = ContentValues().apply {
        put("id", id)
        put("name", name)
        put("starts_at_epoch_millis", parseIsoOffsetDateTime(startsAt))
        put("venue", venue)
        put("unit_price_cents", unitPriceCents)
        put("capacity", capacity)
    }

    // java.time só existe a partir da API 26; o padrão `XXX` do SimpleDateFormat existe desde a 24.
    private fun parseIsoOffsetDateTime(text: String): Long =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).parse(text)!!.time

    @Serializable
    private class SeedEvent(
        val id: String,
        val name: String,
        val startsAt: String,
        val venue: String,
        val unitPriceCents: Long,
        val capacity: Int,
    )

    private companion object {
        const val SEED_FILE = "events.json"
    }
}

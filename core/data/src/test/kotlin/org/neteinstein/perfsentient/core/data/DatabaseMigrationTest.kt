package org.neteinstein.perfsentient.core.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.perfsentient.core.data.local.ALL_MIGRATIONS
import org.neteinstein.perfsentient.core.data.local.PerfsentienceDatabase
import org.neteinstein.perfsentient.core.data.local.entity.NotificationCountEntity
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Builds a real version-1 database from the exported schema, then lets Room open it with the
 * migrations. Room validates the migrated schema against the entities and throws if they differ.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class DatabaseMigrationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val dbFile: File get() = context.getDatabasePath(PerfsentienceDatabase.NAME)

    @After
    fun tearDown() {
        context.deleteDatabase(PerfsentienceDatabase.NAME)
    }

    @Test
    fun `migration 1 to 2 keeps reports and moves daily notification counts to hourly buckets`() = runTest {
        createDatabase(version = 1) { db ->
            db.execSQL("INSERT INTO notification_counts(day, packageName, count) VALUES('2026-11-16', 'spam', 42)")
            db.execSQL("INSERT INTO reports(day, generatedAt, score, payload) VALUES('2026-11-16', 1, 90, '{}')")
        }

        val database = Room.databaseBuilder(context, PerfsentienceDatabase::class.java, PerfsentienceDatabase.NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val dayStart = LocalDate.of(2026, 11, 16).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            assertEquals(
                listOf(NotificationCountEntity(dayStart, "spam", 42)),
                database.notificationCountDao().between(dayStart, dayStart),
            )
            assertEquals("2026-11-16", database.reportDao().observeLatest().first()?.day)
        } finally {
            database.close()
        }
    }

    private fun createDatabase(version: Int, seed: (SQLiteDatabase) -> Unit) {
        val schema = Json.parseToJsonElement(File("$SCHEMAS/$version.json").readText()).jsonObject.getValue("database").jsonObject
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            schema.getValue("entities").jsonArray.map { it.jsonObject }.forEach { entity ->
                val table = entity.string("tableName")
                db.execSQL(entity.string("createSql").replace(TABLE_PLACEHOLDER, table))
                entity["indices"]?.jsonArray?.forEach { index ->
                    db.execSQL(index.jsonObject.string("createSql").replace(TABLE_PLACEHOLDER, table))
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            seed(db)
            db.version = version
        }
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private companion object {
        const val SCHEMAS = "schemas/org.neteinstein.perfsentient.core.data.local.PerfsentienceDatabase"
        const val TABLE_PLACEHOLDER = "\${TABLE_NAME}"
    }
}

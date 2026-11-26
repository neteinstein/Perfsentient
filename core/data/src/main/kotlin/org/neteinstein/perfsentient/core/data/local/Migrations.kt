package org.neteinstein.perfsentient.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 stores notification counts per hour instead of per day, so a report can cover any 24-hour
 * window. Each old daily count moves to the first hour of its day (UTC midnight) so nothing is lost.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notification_counts_new` (`hourStartMs` INTEGER NOT NULL, " +
                "`packageName` TEXT NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`hourStartMs`, `packageName`))",
        )
        db.execSQL(
            "INSERT INTO `notification_counts_new` (`hourStartMs`, `packageName`, `count`) " +
                "SELECT CAST(strftime('%s', `day`) AS INTEGER) * 1000, `packageName`, `count` FROM `notification_counts`",
        )
        db.execSQL("DROP TABLE `notification_counts`")
        db.execSQL("ALTER TABLE `notification_counts_new` RENAME TO `notification_counts`")
    }
}

/** v3 records the battery charge counter with each vital sample, for finer battery estimates. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `vital_samples` ADD COLUMN `chargeCounterUah` INTEGER")
    }
}

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

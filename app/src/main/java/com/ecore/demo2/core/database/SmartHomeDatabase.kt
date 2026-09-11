package com.ecore.demo2.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ecore.demo2.core.database.dao.AlertDao
import com.ecore.demo2.core.database.dao.HistoryDao
import com.ecore.demo2.core.database.dao.ReadingDao
import com.ecore.demo2.core.database.entity.AlertEntity
import com.ecore.demo2.core.database.entity.ElectricalReadingEntity
import com.ecore.demo2.core.database.entity.HistoryRecordEntity
import com.ecore.demo2.core.database.entity.WaterReadingEntity

/**
 * Caché local. Es solo una caché: si cambia el esquema se recrea (ver DatabaseModule),
 * así que no hacen falta migraciones en esta fase.
 */
@Database(
    entities = [
        ElectricalReadingEntity::class,
        WaterReadingEntity::class,
        HistoryRecordEntity::class,
        AlertEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SmartHomeDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
    abstract fun historyDao(): HistoryDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val NAME = "smart_home_cache.db"
    }
}

package com.ecore.demo2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ecore.demo2.core.database.entity.AlertEntity
import com.ecore.demo2.core.database.entity.ElectricalReadingEntity
import com.ecore.demo2.core.database.entity.HistoryRecordEntity
import com.ecore.demo2.core.database.entity.WaterReadingEntity

@Dao
interface ReadingDao {
    @Insert
    suspend fun insertElectrical(entity: ElectricalReadingEntity)

    @Query("SELECT * FROM electrical_readings ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun latestElectrical(): ElectricalReadingEntity?

    @Query(
        "DELETE FROM electrical_readings WHERE id NOT IN " +
            "(SELECT id FROM electrical_readings ORDER BY timestampMillis DESC LIMIT :keep)",
    )
    suspend fun trimElectrical(keep: Int)

    @Query("DELETE FROM electrical_readings")
    suspend fun clearElectrical()

    @Insert
    suspend fun insertWater(entity: WaterReadingEntity)

    @Query("SELECT * FROM water_readings ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun latestWater(): WaterReadingEntity?

    @Query(
        "DELETE FROM water_readings WHERE id NOT IN " +
            "(SELECT id FROM water_readings ORDER BY timestampMillis DESC LIMIT :keep)",
    )
    suspend fun trimWater(keep: Int)

    @Query("DELETE FROM water_readings")
    suspend fun clearWater()
}

@Dao
abstract class HistoryDao {
    @Query("SELECT * FROM history_records WHERE period = :period ORDER BY timestampMillis")
    abstract suspend fun getByPeriod(period: String): List<HistoryRecordEntity>

    @Query("DELETE FROM history_records WHERE period = :period")
    protected abstract suspend fun deleteByPeriod(period: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(records: List<HistoryRecordEntity>)

    @Transaction
    open suspend fun replacePeriod(period: String, records: List<HistoryRecordEntity>) {
        deleteByPeriod(period)
        insertAll(records)
    }

    @Query("DELETE FROM history_records")
    abstract suspend fun clear()
}

@Dao
abstract class AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestampMillis DESC")
    abstract suspend fun getAll(): List<AlertEntity>

    @Query("SELECT COUNT(*) FROM alerts WHERE isRead = 0")
    abstract suspend fun countUnread(): Int

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    abstract suspend fun markRead(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(alerts: List<AlertEntity>)

    @Query("DELETE FROM alerts")
    abstract suspend fun clear()

    @Transaction
    open suspend fun replaceAll(alerts: List<AlertEntity>) {
        clear()
        insertAll(alerts)
    }
}

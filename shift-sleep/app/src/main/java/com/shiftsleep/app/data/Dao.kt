package com.shiftsleep.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY date ASC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: String, to: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE date = :date LIMIT 1")
    suspend fun get(date: String): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY date ASC")
    suspend fun getAll(): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shift: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(shifts: List<ShiftEntity>)

    @Query("DELETE FROM shifts WHERE date = :date")
    suspend fun delete(date: String)
}

@Dao
interface PrefsDao {
    @Query("SELECT * FROM user_prefs WHERE id = 1")
    fun observe(): Flow<UserPrefsEntity?>

    @Query("SELECT * FROM user_prefs WHERE id = 1")
    suspend fun get(): UserPrefsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(prefs: UserPrefsEntity)
}

package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface BandDao {
    // Member Operations
    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberById(id: Int)

    // Commitment Operations
    @Query("SELECT * FROM commitments ORDER BY id DESC")
    fun getAllCommitments(): Flow<List<Commitment>>

    @Query("SELECT * FROM commitments WHERE id = :id")
    suspend fun getCommitmentById(id: Int): Commitment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: Commitment): Long

    @Update
    suspend fun updateCommitment(commitment: Commitment)

    @Query("DELETE FROM commitments WHERE id = :id")
    suspend fun deleteCommitmentById(id: Int)

    // Attendance/Payment Operations
    @Query("SELECT * FROM attendance WHERE commitmentId = :commitmentId")
    fun getAttendanceForCommitment(commitmentId: Int): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE commitmentId = :commitmentId")
    suspend fun deleteAttendanceForCommitment(commitmentId: Int)
}

@Database(entities = [Member::class, Commitment::class, Attendance::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bandDao(): BandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "band_puno_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // We pre-populate the database inside the repository or helper upon first launch.
            }
        }
    }
}

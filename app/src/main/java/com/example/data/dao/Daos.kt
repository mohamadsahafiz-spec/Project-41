package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AchievementEntity
import com.example.data.model.CommitmentEntity
import com.example.data.model.DebtEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.MonthlyCheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE monthKey = :monthKey ORDER BY timestamp DESC")
    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE monthKey = :monthKey ORDER BY timestamp DESC")
    suspend fun getExpensesForMonthDirect(monthKey: String): List<ExpenseEntity>

    @Query("SELECT SUM(amount) FROM expenses WHERE monthKey = :monthKey")
    suspend fun getTotalExpenseForMonth(monthKey: String): Double?

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getTotalExpenseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM commitments ORDER BY id ASC")
    fun getAllCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments ORDER BY id ASC")
    suspend fun getAllCommitmentsDirect(): List<CommitmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(commitment: CommitmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commitments: List<CommitmentEntity>)

    @Update
    suspend fun update(commitment: CommitmentEntity)

    @Query("UPDATE commitments SET defaultAmount = :newDefault WHERE id = :id")
    suspend fun updateDefaultAmount(id: Long, newDefault: Double)
}

@Dao
interface MonthlyCheckInDao {
    @Query("SELECT * FROM monthly_checkins WHERE monthKey = :monthKey")
    fun getCheckInsForMonth(monthKey: String): Flow<List<MonthlyCheckInEntity>>

    @Query("SELECT * FROM monthly_checkins WHERE monthKey = :monthKey")
    suspend fun getCheckInsForMonthDirect(monthKey: String): List<MonthlyCheckInEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(checkIn: MonthlyCheckInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(checkIns: List<MonthlyCheckInEntity>)

    @Update
    suspend fun update(checkIn: MonthlyCheckInEntity)

    @Query("SELECT COUNT(*) FROM monthly_checkins WHERE monthKey = :monthKey AND isPaid = 1")
    suspend fun getPaidCountForMonth(monthKey: String): Int
}

@Dao
interface FinancialProfileDao {
    @Query("SELECT * FROM financial_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<FinancialProfileEntity?>

    @Query("SELECT * FROM financial_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): FinancialProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: FinancialProfileEntity)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY id ASC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY id ASC")
    suspend fun getAllDebtsDirect(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE `key` = :key LIMIT 1")
    suspend fun getDebtByKey(key: String): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debts: List<DebtEntity>)

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("UPDATE debts SET currentBalance = MAX(0.0, currentBalance - :amount) WHERE `key` = :key")
    suspend fun reduceDebtBalance(key: String, amount: Double)

    @Query("UPDATE debts SET currentBalance = :newBalance WHERE `key` = :key")
    suspend fun setDebtBalance(key: String, newBalance: Double)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, key ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedTimestamp DESC LIMIT 1")
    fun getLatestUnlockedAchievement(): Flow<AchievementEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedTimestamp = :timestamp WHERE `key` = :key AND isUnlocked = 0")
    suspend fun unlock(key: String, timestamp: Long): Int
}

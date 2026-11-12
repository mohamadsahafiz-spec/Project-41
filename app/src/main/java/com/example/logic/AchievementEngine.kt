package com.example.logic

import com.example.data.dao.AchievementDao
import com.example.data.model.DebtEntity
import com.example.data.model.FinancialProfileEntity

object AchievementEngine {

    suspend fun checkAndUnlockAchievements(
        achievementDao: AchievementDao,
        profile: FinancialProfileEntity,
        debts: List<DebtEntity>,
        totalExpensesCount: Int
    ) {
        val now = System.currentTimeMillis()
        val totalDebt = debts.sumOf { it.currentBalance }
        val highInterestDebt = debts.filter { it.isHighInterest }.sumOf { it.currentBalance }
        val accessibleSavings = profile.totalAccessibleSavings
        val totalKwsp = profile.totalKwsp

        val sloan = debts.firstOrNull { it.key == "SHOPEE_SLOAN" }
        val cc = debts.firstOrNull { it.key == "MAYBANK_CC" }
        val hp1 = debts.firstOrNull { it.key == "HL_HP1" }
        val hp2 = debts.firstOrNull { it.key == "AMBANK_HP2" }

        // 1. First Expense
        if (totalExpensesCount > 0) {
            achievementDao.unlock("FIRST_EXPENSE", now)
        }

        // 2. Starter buffer (RM1,000)
        if (accessibleSavings >= 1000.0) {
            achievementDao.unlock("STARTER_EMERGENCY", now)
        }

        // 3. Dedicated savings >= RM500
        if (profile.dedicatedSavings >= 500.0 || accessibleSavings >= 1500.0) {
            achievementDao.unlock("SAVINGS_500", now)
        }

        // 4. Shopee SLoan cleared
        if (sloan != null && sloan.currentBalance <= 0.01) {
            achievementDao.unlock("SLOAN_CLEARED", now)
        }

        // 5. Maybank Credit Card cleared
        if (cc != null && cc.currentBalance <= 0.01) {
            achievementDao.unlock("CREDIT_CARD_CLEARED", now)
        }

        // 6. High-Interest Consumer Debt Free (Step 3 completed)
        if (highInterestDebt <= 0.01) {
            achievementDao.unlock("CONSUMER_DEBT_FREE", now)
        }

        // 7. 1-Month Emergency Fund (~RM3,300)
        if (accessibleSavings >= 3300.0) {
            achievementDao.unlock("ONE_MONTH_EMERGENCY", now)
        }

        // 8. 3-Month Emergency Fund (~RM10,000)
        if (accessibleSavings >= 10000.0) {
            achievementDao.unlock("THREE_MONTH_EMERGENCY", now)
        }

        // 9. Hong Leong Car HP1 cleared
        if (hp1 != null && hp1.currentBalance <= 0.01) {
            achievementDao.unlock("HP1_CLEARED", now)
        }

        // 10. AmBank Car HP2 cleared
        if (hp2 != null && hp2.currentBalance <= 0.01) {
            achievementDao.unlock("HP2_CLEARED", now)
        }

        // 11. 100% Debt Free (Step 5 completed)
        if (totalDebt <= 0.01) {
            achievementDao.unlock("TOTAL_DEBT_FREE", now)
        }

        // 12. 6-Month Emergency Fund (~RM20,000)
        if (accessibleSavings >= 20000.0) {
            achievementDao.unlock("SIX_MONTH_EMERGENCY", now)
        }

        // 13. KWSP Milestone >= RM100,000
        if (totalKwsp >= 100000.0) {
            achievementDao.unlock("KWSP_100K", now)
        }

        // 14. Financial Freedom Foundation (Step 6 reached)
        if (highInterestDebt <= 0.01 && accessibleSavings >= 10000.0) {
            achievementDao.unlock("FINANCIAL_FREEDOM_FOUNDATION", now)
        }
    }
}

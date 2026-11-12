package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.QuickExpenseActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val appContext = context.applicationContext

        // 1. Immediately apply initial layout synchronously so launcher has a rendered view instantly
        for (appWidgetId in appWidgetIds) {
            val initialViews = buildViews(appContext, DateUtils.getCurrentMonthKey(), 0.0, 0)
            appWidgetManager.updateAppWidget(appWidgetId, initialViews)
        }

        // 2. Fetch fresh DB totals in managed background coroutine with goAsync()
        val pendingResult = goAsync()
        val targetIds = appWidgetIds.clone()
        refreshScope.launch {
            try {
                refreshMutex.withLock {
                    performWidgetUpdate(appContext, targetIds)
                }
            } finally {
                try {
                    pendingResult.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val pendingResult = goAsync()
            updateAllWidgets(context, pendingResult)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val pendingResult = goAsync()
        updateAllWidgets(context, pendingResult)
    }

    companion object {
        const val ACTION_ADD_EXPENSE = "com.aistudio.freedomtracker.ACTION_ADD_EXPENSE"
        const val ACTION_REFRESH_WIDGET = "com.aistudio.project41.REFRESH_WIDGET"

        private val refreshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val refreshMutex = Mutex()

        fun buildViews(context: Context, monthKey: String, total: Double, count: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_expense)
            val formattedMonth = DateUtils.formatMonthKey(monthKey)

            views.setTextViewText(R.id.widget_title, "PROJECT 41")
            views.setTextViewText(R.id.widget_month, formattedMonth)
            views.setTextViewText(R.id.widget_amount, DateUtils.formatCurrency(total))
            views.setTextViewText(
                R.id.widget_count,
                "$count ${if (count == 1) "expense" else "expenses"} recorded"
            )

            // 1. Intent to open compact floating QuickExpenseActivity directly (does NOT open MainActivity)
            val quickExpenseIntent = Intent(context, QuickExpenseActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val quickExpensePendingIntent = PendingIntent.getActivity(
                context,
                2002,
                quickExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 2. Intent to open full Project 41 app when tapping widget background/title
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                2001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_btn_add, quickExpensePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_text, quickExpensePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            return views
        }

        /**
         * Safely performs database fetch and updates RemoteViews.
         * Serialized via refreshMutex to prevent race conditions or duplicate overlapping queries.
         */
        private suspend fun performWidgetUpdate(appContext: Context, widgetIds: IntArray? = null) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
                val targetIds = widgetIds ?: run {
                    val thisWidget = ComponentName(appContext, ExpenseWidgetProvider::class.java)
                    appWidgetManager.getAppWidgetIds(thisWidget)
                }
                if (targetIds.isEmpty()) return

                val db = AppDatabase.getDatabase(appContext)
                val monthKey = DateUtils.getCurrentMonthKey()
                val expenses = db.expenseDao().getExpensesForMonthDirect(monthKey)
                val total = expenses.sumOf { it.amount }
                val count = expenses.size

                for (widgetId in targetIds) {
                    val views = buildViews(appContext, monthKey, total, count)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Updates all instances of ExpenseWidgetProvider.
         * When triggered by a BroadcastReceiver, pass [pendingResult] to guarantee completion via goAsync().
         * When triggered by repository/UI coroutines, [pendingResult] defaults to null.
         */
        fun updateAllWidgets(context: Context, pendingResult: BroadcastReceiver.PendingResult? = null) {
            val appContext = context.applicationContext
            refreshScope.launch {
                try {
                    refreshMutex.withLock {
                        performWidgetUpdate(appContext)
                    }
                } finally {
                    try {
                        pendingResult?.finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

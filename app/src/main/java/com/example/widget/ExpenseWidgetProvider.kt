package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val monthKey = DateUtils.getCurrentMonthKey()
                val expenses = db.expenseDao().getExpensesForMonthDirect(monthKey)
                val total = expenses.sumOf { it.amount }
                val count = expenses.size

                for (appWidgetId in appWidgetIds) {
                    val views = buildViews(context, monthKey, total, count)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == ACTION_REFRESH_WIDGET
        ) {
            updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
    }

    companion object {
        const val ACTION_ADD_EXPENSE = "com.aistudio.freedomtracker.ACTION_ADD_EXPENSE"
        const val ACTION_REFRESH_WIDGET = "com.aistudio.project41.REFRESH_WIDGET"

        fun buildViews(context: Context, monthKey: String, total: Double, count: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_expense)
            val formattedMonth = DateUtils.formatMonthKey(monthKey)

            views.setTextViewText(R.id.widget_title, "PROJECT 41")
            views.setTextViewText(R.id.widget_month, formattedMonth)
            views.setTextViewText(R.id.widget_amount, DateUtils.formatCurrency(total))
            views.setTextViewText(
                R.id.widget_count,
                "$count ${if (count == 1) "entry" else "entries"} recorded this month"
            )

            // Intent to open quick expense dialog directly in MainActivity
            val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_ADD_EXPENSE
                putExtra("OPEN_EXPENSE_DIALOG", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                2001,
                addExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_btn_add, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, ExpenseWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (allWidgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val monthKey = DateUtils.getCurrentMonthKey()
                    val expenses = db.expenseDao().getExpensesForMonthDirect(monthKey)
                    val total = expenses.sumOf { it.amount }
                    val count = expenses.size

                    for (widgetId in allWidgetIds) {
                        val views = buildViews(context, monthKey, total, count)
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

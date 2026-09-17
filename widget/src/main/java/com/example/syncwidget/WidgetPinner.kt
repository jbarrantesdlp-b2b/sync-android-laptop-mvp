package com.example.syncwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetPinner {
    enum class Kind { Status, Actions, Compact }

    fun isSupported(context: Context): Boolean {
        return AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }

    fun pin(context: Context, kind: Kind): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val receiver = when (kind) {
            Kind.Status -> SyncWidgetReceiver::class.java
            Kind.Actions -> ActionsWidgetReceiver::class.java
            Kind.Compact -> CompactWidgetReceiver::class.java
        }
        return try {
            manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
        } catch (_: Exception) {
            false
        }
    }
}

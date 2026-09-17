package com.example.syncapp

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory

object BrandWallpaper {
    fun apply(context: Context, resId: Int, flags: Int): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return false
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, flags)
            true
        } catch (_: Exception) {
            false
        }
    }
}

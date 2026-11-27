package com.mshmsdin.captionsearch

import android.app.Application

class CaptionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // تحميل SQLite المدمجة مع FTS5 (بدل SQLite الخاص بالجهاز)
        System.setProperty("org.sqlite.lib.path", applicationInfo.nativeLibraryDir)
        System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.so")
    }
}

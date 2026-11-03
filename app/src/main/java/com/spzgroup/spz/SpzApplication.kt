package com.spzgroup.spz

import android.app.Application
import com.spzgroup.spz.data.AppDatabase
import com.spzgroup.spz.data.AppRepository

class SpzApplication : Application() {

    val repository: AppRepository by lazy {
        AppRepository(AppDatabase.create(this), this)
    }
}
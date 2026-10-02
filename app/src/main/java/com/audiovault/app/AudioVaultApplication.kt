package com.audiovault.app

import android.app.Application
import com.audiovault.app.di.AppContainer
import com.audiovault.app.di.DefaultAppContainer

class AudioVaultApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

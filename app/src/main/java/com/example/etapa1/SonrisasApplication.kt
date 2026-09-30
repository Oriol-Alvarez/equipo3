package com.example.etapa1

import android.app.Application
import com.example.etapa1.di.AppContainer
import com.example.etapa1.di.DefaultAppContainer

class SonrisasApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}

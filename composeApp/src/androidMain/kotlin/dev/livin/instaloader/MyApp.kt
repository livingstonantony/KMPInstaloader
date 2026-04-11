package dev.livin.instaloader

import android.app.Application
import dev.livin.instaloader.utils.appContext

class MyApp: Application()  {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
package com.example.yidianmofa.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.yidianmofa.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}

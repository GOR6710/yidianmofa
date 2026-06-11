package com.example.yidianmofa

import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.example.yidianmofa.content.HomePage

fun mainApp(scope: SpatialAppScope) =
    with(scope) { DefaultWindowContainer { PicoTheme { HomePage() } } }
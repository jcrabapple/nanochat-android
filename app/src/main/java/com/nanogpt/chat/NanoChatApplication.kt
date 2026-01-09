package com.nanogpt.chat

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NanoChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Coil with SVG support
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()

        Coil.setImageLoader(imageLoader)
    }
}

package com.app.multiplatform

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

class DefaultViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MyApplication",
    ) {
        val viewModelStoreOwner = remember { DefaultViewModelStoreOwner() }

        CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
            App()
        }
    }
}
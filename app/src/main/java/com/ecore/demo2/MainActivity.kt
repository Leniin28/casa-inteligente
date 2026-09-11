package com.ecore.demo2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.ui.theme.SmartHomeTheme
import com.ecore.demo2.navigation.AppViewModel
import com.ecore.demo2.navigation.SmartHomeApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val isLoggedIn by appViewModel.isLoggedIn.collectAsStateWithLifecycle()
            SmartHomeTheme(themeMode = themeMode) {
                SmartHomeApp(isLoggedIn = isLoggedIn)
            }
        }
    }
}

package com.android.argosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.android.argosapp.data.session.AuthSessionManager
import com.android.argosapp.data.session.FirebaseAuthSessionManager
import com.android.argosapp.data.session.SharedPreferencesAuthTokenStorage
import com.android.argosapp.ui.navigation.ArgosNavHost
import com.android.argosapp.ui.theme.ArgosappTheme

class MainActivity : ComponentActivity() {
    private val authTokenStorage by lazy {
        SharedPreferencesAuthTokenStorage(applicationContext)
    }
    private val authSessionManager: AuthSessionManager by lazy {
        FirebaseAuthSessionManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ArgosappTheme {
                ArgosNavHost()
            }
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations && authTokenStorage.read().isNullOrBlank()) {
            authSessionManager.clear()
        }
        super.onDestroy()
    }
}

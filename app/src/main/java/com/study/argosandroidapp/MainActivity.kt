package com.study.argosandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.study.argosandroidapp.ui.theme.screen.mockup.AuthTestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Testar auth do firebase:
        //
        // setContent {
        //    AuthTestScreen()
        // }
    }
}
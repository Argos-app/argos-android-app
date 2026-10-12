package com.android.argosapp.viewmodel

import androidx.lifecycle.ViewModel
import com.android.argosapp.util.auth.AuthManager

class ProfileViewModel(
    private val authManager: AuthManager
): ViewModel()
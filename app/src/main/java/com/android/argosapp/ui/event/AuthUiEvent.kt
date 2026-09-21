package com.android.argosapp.ui.event

import com.android.argosapp.auth.error.AuthErrorCode

sealed interface AuthUiEvent {
    data class ShowError(val code: AuthErrorCode) : AuthUiEvent
}

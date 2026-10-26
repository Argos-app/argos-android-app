package com.android.argosapp.dto
import com.google.firebase.messaging.RemoteMessage

data class ArgosNotification(
    val title: String?,
    val type: String?,
    val body: String?
) {
    companion object {
        fun fromRemoteMessage(message: RemoteMessage): ArgosNotification{
            return ArgosNotification(
                title = message.notification?.title,
                type = message.data["type"].orEmpty(),
                body = message.notification?.body
            )
        }

    }
}
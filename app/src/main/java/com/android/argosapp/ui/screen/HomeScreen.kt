package com.android.argosapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.argosapp.model.User
import com.android.argosapp.ui.component.ArgosAuthHeader

@Composable
fun HomeScreen(
    user: User?,
    bearerToken: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 28.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ArgosAuthHeader(subtitle = "Sessão autenticada")

        Spacer(modifier = Modifier.height(56.dp))

        Text(
            text = "Informações da sessão",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        SessionInformation(label = "Usuário", value = user?.userName)
        SessionInformation(label = "E-mail", value = user?.userEmail)
        SessionInformation(label = "Fazenda", value = user?.farmName)
        SessionInformation(label = "Bearer token", value = bearerToken)
    }
}

@Composable
private fun SessionInformation(
    label: String,
    value: String?,
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
        Text(
            text = value.orEmpty().ifBlank { "Não disponível" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colors.onBackground,
                fontSize = 15.sp,
            ),
        )
        Spacer(modifier = Modifier.height(18.dp))
    }
}

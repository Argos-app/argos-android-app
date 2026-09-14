package com.android.argosapp.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.android.argosapp.model.User
import com.android.argosapp.ui.component.ArgosAuthHeader
import com.android.argosapp.ui.component.ArgosScreen
import com.android.argosapp.ui.theme.ArgosSpacing
import com.android.argosapp.ui.theme.argosFieldLabel
import com.android.argosapp.ui.theme.argosScreenTitle
import com.android.argosapp.ui.theme.argosBody

@Composable
fun HomeScreen(
    user: User?,
    bearerToken: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    ArgosScreen(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalPadding = ArgosSpacing.section,
    ) {
        ArgosAuthHeader(subtitle = "Sessão autenticada")

        Spacer(modifier = Modifier.height(ArgosSpacing.section))

        Text(
            text = "Informações da sessão",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.argosScreenTitle.copy(
                color = colors.primary,
            ),
        )

        Spacer(modifier = Modifier.height(ArgosSpacing.xl))

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
            style = MaterialTheme.typography.argosFieldLabel.copy(
                color = colors.primary,
            ),
        )
        Text(
            text = value.orEmpty().ifBlank { "Não disponível" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.argosBody.copy(
                color = colors.onBackground,
            ),
        )
        Spacer(modifier = Modifier.height(ArgosSpacing.lg))
    }
}

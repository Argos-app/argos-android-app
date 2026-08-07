package com.study.argosandroidapp.ui.theme.screen.mockup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import com.study.argosandroidapp.R
import com.study.argosandroidapp.data.repository.FirebaseAuthRepository
import com.study.argosandroidapp.presentation.GoogleSignInHelper

import kotlinx.coroutines.launch

@Composable
fun AuthTestScreen() {
    val context = LocalContext.current
    val repository = remember { FirebaseAuthRepository() }
    val scope = rememberCoroutineScope()

    val googleHelper = remember {
        GoogleSignInHelper(
            context = context,
            webClientId = context.getString(R.string.default_web_client_id),
            scope = scope
        )
    }

    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("aguardando...") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Auth Test", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    try {
                        val result = repository.register(nome, email, senha)
                        log = "register ok\nuid: ${result.user?.uid}"
                    } catch (e: Exception) {
                        log = "register erro: ${e.message}"
                    }
                }
            }) { Text("Register") }

            Button(onClick = {
                scope.launch {
                    try {
                        val result = repository.login(email, senha)
                        log = "login ok\nuid: ${result.user?.uid}"
                    } catch (e: Exception) {
                        log = "login erro: ${e.message}"
                    }
                }
            }) { Text("Login") }

            Button(onClick = {
                scope.launch {
                    repository.logout()
                    log = "logout ok"
                }
            }) { Text("Logout") }
        }

        Button(
            onClick = {
                googleHelper.launch(
                    onSuccess = { idToken ->
                        scope.launch {
                            try {
                                val result = repository.googleLogin(idToken)
                                log = "google ok\nuid: ${result.user?.uid}\nnome: ${result.user?.displayName}"
                            } catch (e: Exception) {
                                log = "google erro: ${e.message}"
                            }
                        }
                    },
                    onError = { e ->
                        log = "google erro: ${e.message}"
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Entrar com Google") }

        Button(
            onClick = {
                scope.launch {
                    val user = repository.getCurrentUser()
                    log = if (user != null)
                        "sessão ativa\nuid: ${user.uid}\nnome: ${user.displayName}"
                    else
                        "nenhuma sessão ativa"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Verificar sessão") }

        HorizontalDivider()

        Text(
            text = log,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
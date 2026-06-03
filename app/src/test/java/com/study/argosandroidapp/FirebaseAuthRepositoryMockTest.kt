package com.study.argosandroidapp

import com.study.argosandroidapp.mokup.FirebaseAuthRepositoryMock
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

import io.mockk.mockk

import kotlinx.coroutines.test.runTest

/**
 * Testes unitários para [FirebaseAuthRepositoryMock].
 *
 * Valida que o mockup se comporta conforme configurado —
 * retornando sucesso, lançando exceções e expondo o usuário atual.
 */
class FirebaseAuthRepositoryMockTest {

    private lateinit var repository: FirebaseAuthRepositoryMock

    @Before
    fun setup() {
        repository = FirebaseAuthRepositoryMock()
    }

    @Test
    fun `login retorna AuthResult quando configurado com sucesso`() = runTest {
        val authResultMock = mockk<AuthResult>(relaxed = true)
        repository.loginResult = Result.success(authResultMock)

        val result = repository.login("email@teste.com", "123456")

        assertNotNull(result)
        assertEquals(authResultMock, result)
    }

    @Test
    fun `login lanca excecao quando configurado com falha`() = runTest {
        repository.loginResult = Result.failure(Exception("Senha incorreta"))

        assertThrows(Exception::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.login("email@teste.com", "errada")
            }
        }
    }

    @Test
    fun `register retorna AuthResult quando configurado com sucesso`() = runTest {
        val authResultMock = mockk<AuthResult>(relaxed = true)
        repository.registerResult = Result.success(authResultMock)

        val result = repository.register("João", "joao@teste.com", "123456")

        assertNotNull(result)
        assertEquals(authResultMock, result)
    }

    @Test
    fun `register lanca excecao quando configurado com falha`() = runTest {
        repository.registerResult = Result.failure(Exception("Email já cadastrado"))

        assertThrows(Exception::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.register("João", "joao@teste.com", "123456")
            }
        }
    }

    @Test
    fun `googleLogin retorna AuthResult quando configurado com sucesso`() = runTest {
        val authResultMock = mockk<AuthResult>(relaxed = true)
        repository.googleLoginResult = Result.success(authResultMock)

        val result = repository.googleLogin("token-valido")

        assertNotNull(result)
        assertEquals(authResultMock, result)
    }

    @Test
    fun `googleLogin lanca excecao quando configurado com falha`() = runTest {
        repository.googleLoginResult = Result.failure(Exception("Token inválido"))

        assertThrows(Exception::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.googleLogin("token-invalido")
            }
        }
    }

    @Test
    fun `getCurrentUser retorna null por padrao`() = runTest {
        val user = repository.getCurrentUser()
        assertNull(user)
    }

    @Test
    fun `getCurrentUser retorna usuario quando configurado`() = runTest {
        val userMock = mockk<FirebaseUser>(relaxed = true)
        repository.currentUser = userMock

        val user = repository.getCurrentUser()

        assertNotNull(user)
        assertEquals(userMock, user)
    }

    @Test
    fun `logout executa sem excecao`() = runTest {
        repository.logout()
    }
}
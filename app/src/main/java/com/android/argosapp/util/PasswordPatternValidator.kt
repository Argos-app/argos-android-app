package com.android.argosapp.util

class PasswordPatternValidator {

    companion object {
        const val MINIMAL_LENGTH = 8
        const val MINIMAL_CAPITAL_CHAR_NUMBER = 1
        const val MINIMAL_NOT_CAPITAL_CHAR_NUMBER = 1
        const val MINIMAL_NUMERIC_CHAR_NUMBER = 1
        const val MINIMAL_SPECIAL_CHAR_NUMBER = 1

        val SPECIAL_CHARS = setOf('@', '!', '#', '$', '%', '&')
    }

    /**
     * Função utilitária de validação de senha, retorna um lista <String, Boolean>. Chaves:
     * - length:            Tamanho de senha
     * - minimalCapital:    Quantidade de caracteres maiúsculos
     * - minimalNotCapital: Quantidade de caracteres minúsculos
     * - minimalNumeric:    Quantidade de caracteres numéricos
     * - minimalSpecial:    Quantidade de caracteres especiais
     */
    fun validatePassword(password: String): Map<String, Boolean> {
        return mapOf(
            "length" to (password.length >= MINIMAL_LENGTH),
            "minimalCapital" to (
                password.count { it in 'A'..'Z' } >= MINIMAL_CAPITAL_CHAR_NUMBER
            ),
            "minimalNotCapital" to (
                password.count { it in 'a'..'z' } >= MINIMAL_NOT_CAPITAL_CHAR_NUMBER
            ),
            "minimalNumeric" to (
                password.count { it in '0'..'9' } >= MINIMAL_NUMERIC_CHAR_NUMBER
            ),
            "minimalSpecial" to (
                password.count { it in SPECIAL_CHARS } >= MINIMAL_SPECIAL_CHAR_NUMBER
            ),
        )
    }
}

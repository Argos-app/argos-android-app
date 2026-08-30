package com.android.argosapp.util

class PasswordPatternValidator {

    companion object {
        val MINIMAL_LENGTH = 8
        val MINIMAL_CAPITAL_CHAR_NUMBER = 1
        val MINIMAL_NOT_CAPITAL_CHAR_NUMBER = 1
        val MINIMAL_NUMERIC_CHAR_NUMBER = 1
        val MINIMAL_SPECIAL_CHAR_NUMBER = 1

        val SPECIAL_CHARS = listOf<Char>('@', '!', '#', '$', '%', '&')
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

        val result = HashMap<String, Boolean>()

        result["length"] = password.length >= MINIMAL_LENGTH

        result["minimalCapital"] = password.count(
            { Regex("[A-Z]").matches(it.toString()) }
            ) >= MINIMAL_CAPITAL_CHAR_NUMBER

        result["minimalNotCapital"] = password.count(
            { Regex("[a-z]").matches(it.toString()) }
        ) >= MINIMAL_NOT_CAPITAL_CHAR_NUMBER

        result["minimalNumeric"] = password.count(
            { Regex("[0-9]").matches(it.toString()) }
        ) >= MINIMAL_NUMERIC_CHAR_NUMBER

        result["minimalSpecial"] = password.count(
            { Regex("[${SPECIAL_CHARS.joinToString()}]").matches(it.toString()) }
        ) >= MINIMAL_SPECIAL_CHAR_NUMBER

        return result
    }

}
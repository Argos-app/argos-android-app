package com.android.argosapp.ui.component

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.argosapp.ui.theme.ArgosTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArgosComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun button_invokes_click_callback() {
        var clicked = false

        composeRule.setContent {
            ArgosTheme {
                ArgosButton(
                    text = "Entrar",
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Entrar").performClick()

        assertTrue(clicked)
    }

    @Test
    fun toggle_exposes_and_updates_checked_state() {
        val checked = mutableStateOf(false)

        composeRule.setContent {
            ArgosTheme {
                ArgosToggle(
                    checked = checked.value,
                    onCheckedChange = { checked.value = it },
                )
            }
        }

        composeRule.onNode(isToggleable()).assertIsOff().performClick()
        composeRule.onNode(isToggleable()).assertIsOn()
    }
}

package com.example.etapa1.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.etapa1.domain.Sesion
import com.example.etapa1.model.UserRole
import com.example.etapa1.ui.navigation.SonrisasNavHost
import com.example.etapa1.ui.theme.Etapa1Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navHost_iniciaEnSeleccionDeAulaParaEducadora() {
        val sesionEducadora = Sesion(role = UserRole.EDUCADORA)

        composeTestRule.setContent {
            Etapa1Theme {
                SonrisasNavHost(
                    sesion = sesionEducadora,
                    onSalir = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Aulas").assertIsDisplayed()
    }
}

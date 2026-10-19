package com.example.etapa1.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.ui.screens.LoginScreen
import com.example.etapa1.ui.state.LoginViewModel
import com.example.etapa1.ui.theme.Etapa1Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_muestraTituloYBoton() {
        val mockRepo = MockAuthRepositoryImpl()
        val viewModel = LoginViewModel(mockRepo)

        composeTestRule.setContent {
            Etapa1Theme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
    }
}

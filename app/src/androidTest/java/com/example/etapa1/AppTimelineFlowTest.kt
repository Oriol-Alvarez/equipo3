package com.example.etapa1

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.etapa1.ui.navigation.SonrisasApp
import com.example.etapa1.ui.theme.Etapa1Theme
import org.junit.Rule
import org.junit.Test

class AppTimelineFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun activityAndAttendanceReturnToTheCorrectChildThroughNavigation() {
        compose.setContent { Etapa1Theme { SonrisasApp() } }
        compose.onNodeWithText("Iniciar Sesión").performClick()
        compose.onNodeWithText("Sala 1A").performClick()
        compose.onNodeWithText("Sofía López").performClick()
        compose.onNodeWithContentDescription("Nueva Actividad").performClick()
        // The demo observation must not name Mateo when the activity belongs to Sofía.
        compose.onNode(hasSetTextAction() and hasText("Sofía López", substring = true))
            .performScrollTo().assertExists()
        compose.onNodeWithText("Registrar en Bitácora").performScrollTo().performClick()
        compose.onNodeWithText("Sofía López disfrutó", substring = true).assertExists()

        compose.onNodeWithContentDescription("Pasar Asistencia").performClick()
        repeat(2) {
            compose.onAllNodesWithText("Firma digital aquí")[0]
                .performScrollTo().performTouchInput { swipeLeft() }
        }
        compose.onNodeWithText("Guardar Registro de Ingreso").performScrollTo().performClick()
        compose.onNodeWithText("Asistencia Guardada").assertExists()
        compose.onNodeWithText("Aceptar").performClick()
        compose.onNodeWithText("Ingreso:", substring = true).assertExists()

        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Lucas Martínez").performClick()
        compose.onNodeWithText("Aún no hay registros para Lucas Martínez").assertExists()
        compose.onNodeWithText("Sofía López disfrutó", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Ingreso:", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Sofía López").performClick()
        compose.onNodeWithText("Ingreso:", substring = true).assertExists()
        compose.onNodeWithText("Sofía López disfrutó", substring = true).assertExists()
    }
}

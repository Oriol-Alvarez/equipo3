package com.example.etapa1

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.ui.screens.ChatBitacoraScreen
import com.example.etapa1.ui.state.ChatBitacoraViewModel
import com.example.etapa1.ui.theme.Etapa1Theme
import org.junit.Rule
import org.junit.Test

class ChildTimelineTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unsentDraftDoesNotMoveToAnotherChild() {
        val selectedChild = mutableStateOf(MockDataRepository.mateoGarcia)
        val viewModel = ChatBitacoraViewModel(MockBitacoraRepositoryImpl())
        compose.setContent {
            Etapa1Theme {
                ChatBitacoraScreen(viewModel, selectedChild.value, onBack = {}, onNewActivityClick = {})
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("Borrador de Mateo")
        compose.runOnIdle {
            selectedChild.value = MockDataRepository.childrenSala1A.first { it.id == "sofia_lopez" }
        }
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
        compose.onNodeWithText("Borrador de Mateo").assertDoesNotExist()
    }

    @Test fun messagesStayWithTheirChildWhenSwitchingAndReturning() {
        val sofia = MockDataRepository.childrenSala1A.first { it.id == "sofia_lopez" }
        val selectedChild = mutableStateOf(sofia)
        val viewModel = ChatBitacoraViewModel(MockBitacoraRepositoryImpl())
        compose.setContent {
            Etapa1Theme {
                ChatBitacoraScreen(
                    viewModel = viewModel,
                    child = selectedChild.value,
                    onBack = {},
                    onNewActivityClick = {}
                )
            }
        }
        compose.onNodeWithText("Aún no hay registros para Sofía López").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("Mensaje exclusivo de Sofia")
        compose.onNodeWithContentDescription("Enviar").performClick()
        compose.onNodeWithText("Mensaje exclusivo de Sofia").assertExists()

        compose.runOnIdle { selectedChild.value = MockDataRepository.mateoGarcia }
        compose.onNodeWithText("Mensaje exclusivo de Sofia").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("Mensaje exclusivo de Mateo")
        compose.onNodeWithContentDescription("Enviar").performClick()
        compose.onNodeWithText("Mensaje exclusivo de Mateo").assertExists()

        compose.runOnIdle { selectedChild.value = sofia }
        compose.onNodeWithText("Mensaje exclusivo de Sofia").assertExists()
        compose.onNodeWithText("Mensaje exclusivo de Mateo").assertDoesNotExist()
    }
}

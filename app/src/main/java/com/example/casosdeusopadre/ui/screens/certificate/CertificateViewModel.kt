package com.example.casosdeusopadre.ui.screens.certificate

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.casosdeusopadre.data.models.Child
import com.example.casosdeusopadre.data.models.MedicalCertificate
import com.example.casosdeusopadre.data.repository.ChildcareRepository
import com.example.casosdeusopadre.data.repository.FakeChildcareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CertificateUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val children: List<Child> = emptyList(),
    val selectedChild: Child? = null,
    val activeCertificate: MedicalCertificate? = null,
    val uploadError: String? = null,
    val actionMessage: String? = null
)

class CertificateViewModel(
    private val repository: ChildcareRepository = FakeChildcareRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CertificateUiState())
    val uiState: StateFlow<CertificateUiState> = _uiState.asStateFlow()

    private val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = repository.getCurrentUser()
            if (user != null) {
                val children = repository.getChildrenForParent(user.id)
                val selected = children.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    children = children,
                    selectedChild = selected,
                    isLoading = false
                )
                selected?.let { loadCertificateForChild(it.id) }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, uploadError = "Usuario no autenticado")
            }
        }
    }

    fun selectChild(child: Child) {
        if (_uiState.value.selectedChild?.id != child.id) {
            _uiState.value = _uiState.value.copy(selectedChild = child)
            loadCertificateForChild(child.id)
        }
    }

    private fun loadCertificateForChild(childId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val cert = repository.getMedicalCertificate(childId)
            _uiState.value = _uiState.value.copy(activeCertificate = cert, isLoading = false)
        }
    }

    fun toggleNetworkMode() {
        _uiState.value = _uiState.value.copy(isOnline = !_uiState.value.isOnline)
    }

    fun handleFileSelection(context: Context, uri: Uri?) {
        if (uri == null) return
        
        val child = _uiState.value.selectedChild
        if (child == null) {
            _uiState.value = _uiState.value.copy(uploadError = "Selecciona un niño primero")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, uploadError = null)

            // Validate File
            val isValid = validateFile(context, uri)
            if (!isValid) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            // Upload
            val isOnline = _uiState.value.isOnline
            val result = repository.uploadMedicalCertificate(child.id, uri.toString(), isOnline)
            
            result.fold(
                onSuccess = { cert ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeCertificate = cert,
                        actionMessage = if (isOnline) "Certificado subido exitosamente" else "Certificado guardado localmente. Pendiente de envío."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        uploadError = error.message ?: "Error al subir el certificado"
                    )
                }
            )
        }
    }

    private fun validateFile(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != "application/pdf" && mimeType != "image/jpeg" && mimeType != "image/png") {
            _uiState.value = _uiState.value.copy(uploadError = "Formato inválido. Solo se permite PDF, JPG o PNG.")
            return false
        }

        var size: Long = 0
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex != -1) {
                size = cursor.getLong(sizeIndex)
            }
        }

        if (size > MAX_FILE_SIZE_BYTES) {
            _uiState.value = _uiState.value.copy(uploadError = "El archivo es demasiado grande. Máximo 5MB.")
            return false
        }

        return true
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
    
    fun clearUploadError() {
        _uiState.value = _uiState.value.copy(uploadError = null)
    }
}

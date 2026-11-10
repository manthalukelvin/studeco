package com.celmatech.studeco.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celmatech.studeco.data.model.AuthState
import com.celmatech.studeco.data.repository.AuthRepository
import com.celmatech.studeco.data.repository.FeatureFlagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val featureFlagRepository: FeatureFlagRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState(isLoading = true))

    fun initialize() {
        viewModelScope.launch {
            authRepository.initialize()
            featureFlagRepository.refresh()
        }
    }

    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            if (result.isSuccess) featureFlagRepository.refresh()
            onResult(result)
        }
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        educationLevel: String,
        institution: String,
        programme: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.signUp(email, password, fullName, educationLevel, institution, programme)
            onResult(result)
        }
    }

    fun resetPassword(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(authRepository.resetPassword(email))
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

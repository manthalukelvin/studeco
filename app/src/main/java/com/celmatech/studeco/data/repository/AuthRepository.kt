package com.celmatech.studeco.data.repository

import com.celmatech.studeco.data.model.AuthState
import com.celmatech.studeco.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val _authState = MutableStateFlow(AuthState(isLoading = true))
    val authState: Flow<AuthState> = _authState.asStateFlow()

    suspend fun initialize() {
        try {
            val session = client.auth.currentSessionOrNull()
            if (session?.user != null) {
                val profile = fetchProfile(session.user!!.id)
                _authState.value = AuthState(
                    isAuthenticated = true,
                    isLoading = false,
                    user = profile,
                    needsEmailVerification = session.user?.emailConfirmedAt == null,
                    needsProfileCompletion = profile?.educationLevel.isNullOrBlank() ||
                            profile?.institution.isNullOrBlank()
                )
            } else {
                _authState.value = AuthState(isAuthenticated = false, isLoading = false)
            }
        } catch (e: Exception) {
            _authState.value = AuthState(
                isAuthenticated = false,
                isLoading = false,
                error = e.message
            )
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val session = client.auth.currentSessionOrNull()
                ?: return Result.failure(Exception("No session after sign-in"))
            val profile = fetchProfile(session.user!!.id)
            if (profile?.isSuspended == true) {
                client.auth.signOut()
                return Result.failure(Exception("Your account has been suspended. Contact support."))
            }
            _authState.value = AuthState(
                isAuthenticated = true,
                isLoading = false,
                user = profile,
                needsEmailVerification = session.user?.emailConfirmedAt == null,
                needsProfileCompletion = profile?.educationLevel.isNullOrBlank() ||
                        profile?.institution.isNullOrBlank()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(cleanAuthError(e.message)))
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        educationLevel: String,
        institution: String,
        programme: String = ""
    ): Result<Unit> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                    put("education_level", educationLevel)
                    put("institution", institution)
                    put("programme", programme)
                    put("country", "MW")
                }
            }
            val session = client.auth.currentSessionOrNull()
            if (session?.user != null) {
                val profile = fetchProfile(session.user!!.id)
                _authState.value = AuthState(
                    isAuthenticated = true,
                    isLoading = false,
                    user = profile,
                    needsEmailVerification = session.user?.emailConfirmedAt == null,
                    needsProfileCompletion = false
                )
            } else {
                _authState.value = AuthState(
                    isAuthenticated = false,
                    isLoading = false,
                    needsEmailVerification = true
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(cleanAuthError(e.message)))
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            client.auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(cleanAuthError(e.message)))
        }
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
        }
        _authState.value = AuthState(isAuthenticated = false, isLoading = false)
    }

    suspend fun fetchProfile(userId: String): Profile? {
        return try {
            client.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<Profile>()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshProfile() {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        val profile = fetchProfile(userId)
        _authState.update {
            it.copy(
                user = profile,
                needsProfileCompletion = profile?.educationLevel.isNullOrBlank() ||
                        profile?.institution.isNullOrBlank()
            )
        }
    }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    private fun cleanAuthError(msg: String?): String {
        if (msg == null) return "Authentication failed"
        return when {
            msg.contains("Invalid login", ignoreCase = true) -> "Invalid email or password"
            msg.contains("Email not confirmed", ignoreCase = true) -> "Please verify your email first"
            msg.contains("User already registered", ignoreCase = true) -> "An account with this email already exists"
            msg.contains("Password", ignoreCase = true) && msg.contains("least", ignoreCase = true) ->
                "Password must be at least 6 characters"
            else -> msg.take(120)
        }
    }
}

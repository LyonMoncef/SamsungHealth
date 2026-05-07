package fr.datasaillance.nightfall.data.http

import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class LoginResponse(val access_token: String, val refresh_token: String, val token_type: String, val expires_in: Int)
@Serializable data class RegisterRequest(val email: String, val password: String)
@Serializable data class RegisterResponse(val id: String, val email: String)
@Serializable data class PasswordResetRequest(val email: String)
@Serializable data class StatusResponse(val status: String)
@Serializable data class GoogleStartRequest(val return_to: String? = null)
@Serializable data class GoogleStartResponse(val authorize_url: String)

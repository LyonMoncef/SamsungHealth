package fr.datasaillance.nightfall.viewmodel.auth

// spec: Tests d'acceptation TA-AUTH-01 through TA-AUTH-10
// spec: section "AuthViewModel" + "Tests d'acceptation"
// RED by construction: fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel does not exist yet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
// fr.datasaillance.nightfall.viewmodel.auth.LoginUiState
// fr.datasaillance.nightfall.viewmodel.auth.RegisterUiState
// fr.datasaillance.nightfall.viewmodel.auth.ForgotPasswordUiState
// fr.datasaillance.nightfall.data.http.NightfallApi (auth methods don't exist yet)
// fr.datasaillance.nightfall.data.http.LoginRequest
// fr.datasaillance.nightfall.data.http.LoginResponse
// fr.datasaillance.nightfall.data.http.RegisterRequest
// fr.datasaillance.nightfall.data.http.RegisterResponse
// fr.datasaillance.nightfall.data.http.PasswordResetRequest
// fr.datasaillance.nightfall.data.http.StatusResponse

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var api: fr.datasaillance.nightfall.data.http.NightfallApi
    private lateinit var tokenDataStore: fr.datasaillance.nightfall.data.auth.TokenDataStore
    private lateinit var viewModel: fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel

    @Before
    fun setUp() {
        // spec: TA-AUTH-04 — viewModelScope.launch must use a test dispatcher
        Dispatchers.setMain(testDispatcher)
        api = mock()
        tokenDataStore = mock()
        // spec: D1 — AuthViewModel constructed directly without Hilt (issue #52)
        viewModel = fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel(api, tokenDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // spec: TA-AUTH-01 — login() success → saveToken() called, state is LoginUiState.Success
    @Test
    fun login_success_savesTokenAndEmitsSuccess() = runTest {
        val fakeResponse = fr.datasaillance.nightfall.data.http.LoginResponse(
            access_token = "tok_abc",
            refresh_token = "ref_abc",
            token_type = "bearer",
            expires_in = 3600
        )
        whenever(api.login(any())).thenReturn(fakeResponse)

        viewModel.login("user@example.com", "password123")
        advanceUntilIdle()

        // spec: TA-AUTH-01 — tokenDataStore.saveToken() called with the access_token
        verify(tokenDataStore).saveToken("tok_abc")

        // spec: TA-AUTH-01 — loginState must be LoginUiState.Success
        val state = viewModel.loginState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Success) {
            "loginState must be LoginUiState.Success after successful login — spec: TA-AUTH-01, got: $state"
        }
    }

    // spec: TA-AUTH-02 — login() HTTP 401 → LoginUiState.Error("Email ou mot de passe incorrect")
    @Test
    fun login_http401_emitsErrorInvalidCredentials() = runTest {
        val httpException = mock<HttpException>()
        whenever(httpException.code()).thenReturn(401)
        whenever(api.login(any())).thenThrow(httpException)

        viewModel.login("bad@example.com", "wrongpassword")
        advanceUntilIdle()

        // spec: TA-AUTH-02 — saveToken() must NOT be called on 401
        verify(tokenDataStore, never()).saveToken(any())

        val state = viewModel.loginState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Error) {
            "loginState must be LoginUiState.Error on HTTP 401 — spec: TA-AUTH-02, got: $state"
        }
        val errorMessage = (state as fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Error).message
        assert(errorMessage == "Email ou mot de passe incorrect") {
            "Error message for HTTP 401 must be 'Email ou mot de passe incorrect' — spec: TA-AUTH-02, got: '$errorMessage'"
        }
    }

    // spec: TA-AUTH-03 — login() HTTP 403 → LoginUiState.Error("Email non vérifié — consultez votre boîte mail")
    @Test
    fun login_http403_emitsErrorEmailNotVerified() = runTest {
        val httpException = mock<HttpException>()
        whenever(httpException.code()).thenReturn(403)
        whenever(api.login(any())).thenThrow(httpException)

        viewModel.login("unverified@example.com", "password123")
        advanceUntilIdle()

        // spec: TA-AUTH-03 — saveToken() must NOT be called on 403
        verify(tokenDataStore, never()).saveToken(any())

        val state = viewModel.loginState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Error) {
            "loginState must be LoginUiState.Error on HTTP 403 — spec: TA-AUTH-03, got: $state"
        }
        val errorMessage = (state as fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Error).message
        assert(errorMessage == "Email non vérifié — consultez votre boîte mail") {
            "Error message for HTTP 403 must be 'Email non vérifié — consultez votre boîte mail' — spec: TA-AUTH-03, got: '$errorMessage'"
        }
    }

    // spec: TA-AUTH-04 — loginState is LoginUiState.Loading while request is in-flight
    @Test
    fun login_emitsLoadingWhileInFlight() = runTest {
        val fakeResponse = fr.datasaillance.nightfall.data.http.LoginResponse(
            access_token = "tok",
            refresh_token = "ref",
            token_type = "bearer",
            expires_in = 3600
        )
        whenever(api.login(any())).thenReturn(fakeResponse)

        // spec: TA-AUTH-04 — state must be Loading synchronously before coroutine completes
        viewModel.login("user@example.com", "password123")

        // Before advanceUntilIdle() — coroutine is suspended at the api.login() call
        val stateWhileInFlight = viewModel.loginState.value
        assert(stateWhileInFlight is fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Loading) {
            "loginState must be LoginUiState.Loading immediately after login() call — spec: TA-AUTH-04, got: $stateWhileInFlight"
        }

        advanceUntilIdle()
    }

    // spec: TA-AUTH-05 — register() HTTP 201 → RegisterUiState.Success
    @Test
    fun register_success_emitsSuccess() = runTest {
        val fakeResponse = fr.datasaillance.nightfall.data.http.RegisterResponse(
            id = "user-uuid-123",
            email = "new@example.com"
        )
        whenever(api.register(any(), any())).thenReturn(fakeResponse)

        viewModel.register("new@example.com", "StrongP@ss1", registrationToken = "reg-token")
        advanceUntilIdle()

        // spec: TA-AUTH-05 — registerState must be RegisterUiState.Success
        val state = viewModel.registerState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.RegisterUiState.Success) {
            "registerState must be RegisterUiState.Success on HTTP 201 — spec: TA-AUTH-05, got: $state"
        }
    }

    // spec: TA-AUTH-07 — requestPasswordReset() always → ForgotPasswordUiState.Sent (anti-enum)
    @Test
    fun requestPasswordReset_alwaysEmitsSent_onSuccess() = runTest {
        val fakeResponse = fr.datasaillance.nightfall.data.http.StatusResponse(status = "pending")
        whenever(api.requestPasswordReset(any())).thenReturn(fakeResponse)

        viewModel.requestPasswordReset("any@example.com")
        advanceUntilIdle()

        // spec: TA-AUTH-07 — ForgotPasswordUiState.Sent regardless of backend response
        val state = viewModel.forgotState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.ForgotPasswordUiState.Sent) {
            "forgotState must be ForgotPasswordUiState.Sent on success — spec: TA-AUTH-07, got: $state"
        }
    }

    // spec: TA-AUTH-07 — requestPasswordReset() always → ForgotPasswordUiState.Sent even on exception (anti-enum)
    @Test
    fun requestPasswordReset_alwaysEmitsSent_onNetworkError() = runTest {
        whenever(api.requestPasswordReset(any())).thenThrow(RuntimeException("Network timeout"))

        viewModel.requestPasswordReset("any@example.com")
        advanceUntilIdle()

        // spec: TA-AUTH-07 — anti-enum: Sent even when backend throws (prevents account enumeration)
        val state = viewModel.forgotState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.ForgotPasswordUiState.Sent) {
            "forgotState must be ForgotPasswordUiState.Sent even on network error — spec: TA-AUTH-07, got: $state"
        }
    }

    // spec: TA-AUTH-08 — storeTokenFromCallback("valid_jwt") → saveToken called, state is LoginUiState.Success
    @Test
    fun storeTokenFromCallback_validToken_savesTokenAndEmitsSuccess() {
        viewModel.storeTokenFromCallback("valid_jwt_token_xyz")

        // spec: TA-AUTH-08 — tokenDataStore.saveToken() must be called with the token value
        verify(tokenDataStore).saveToken("valid_jwt_token_xyz")

        // spec: TA-AUTH-08 — loginState must be LoginUiState.Success
        val state = viewModel.loginState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Success) {
            "loginState must be LoginUiState.Success after storeTokenFromCallback — spec: TA-AUTH-08, got: $state"
        }
    }

    // spec: TA-AUTH-09 — The Screen handles null/empty token before calling storeTokenFromCallback.
    // The ViewModel's storeTokenFromCallback(token) with a valid token must call saveToken.
    // Verifying the contract: saveToken is NOT called when the Screen withholds the call.
    // We test initial state: before any storeTokenFromCallback call, saveToken has never been invoked.
    @Test
    fun tokenDataStore_saveToken_notCalled_untilStoreTokenFromCallbackInvoked() {
        // spec: TA-AUTH-09 — saveToken() must NOT be called if the Screen never invokes storeTokenFromCallback
        // (Screen is responsible for guarding against null/empty token)
        verify(tokenDataStore, never()).saveToken(any())

        // spec: TA-AUTH-09 — initial state is LoginUiState.Idle (not Success, not Error)
        val state = viewModel.loginState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.auth.LoginUiState.Idle) {
            "loginState must be LoginUiState.Idle before any action — spec: TA-AUTH-09, got: $state"
        }
    }

    // spec: TA-AUTH-10 — No Timber log contains the JWT value after successful login
    @Test
    fun login_success_doesNotLogTokenValue() = runTest {
        val sensitiveToken = "super-secret-jwt-do-not-log-me-12345"
        val fakeResponse = fr.datasaillance.nightfall.data.http.LoginResponse(
            access_token = sensitiveToken,
            refresh_token = "ref_token",
            token_type = "bearer",
            expires_in = 3600
        )
        whenever(api.login(any())).thenReturn(fakeResponse)

        // Capture Timber logs via a custom tree
        val loggedMessages = mutableListOf<String>()
        val testTree = object : timber.log.Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                loggedMessages.add(message)
                t?.message?.let { loggedMessages.add(it) }
            }
        }
        timber.log.Timber.plant(testTree)

        viewModel.login("user@example.com", "MyPassword123!")
        advanceUntilIdle()

        timber.log.Timber.uproot(testTree)

        // spec: TA-AUTH-10 / D12 — no log line must contain the access_token value
        val leaked = loggedMessages.filter { it.contains(sensitiveToken) }
        assert(leaked.isEmpty()) {
            "Timber logs must NOT contain the JWT access_token value — spec: TA-AUTH-10, D12. Found: $leaked"
        }
    }

    // spec: TA-AUTH-10 — No Timber log contains the password value after login attempt
    @Test
    fun login_doesNotLogPasswordValue() = runTest {
        val sensitivePassword = "MyUniquePassword!999"
        val httpException = mock<HttpException>()
        whenever(httpException.code()).thenReturn(401)
        whenever(api.login(any())).thenThrow(httpException)

        val loggedMessages = mutableListOf<String>()
        val testTree = object : timber.log.Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                loggedMessages.add(message)
            }
        }
        timber.log.Timber.plant(testTree)

        viewModel.login("user@example.com", sensitivePassword)
        advanceUntilIdle()

        timber.log.Timber.uproot(testTree)

        // spec: TA-AUTH-10 / D12 — password must never appear in any log output
        val leaked = loggedMessages.filter { it.contains(sensitivePassword) }
        assert(leaked.isEmpty()) {
            "Timber logs must NOT contain the password value — spec: TA-AUTH-10, D12. Found: $leaked"
        }
    }
}

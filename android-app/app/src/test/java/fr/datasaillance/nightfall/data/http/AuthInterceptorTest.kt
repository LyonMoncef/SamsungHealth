package fr.datasaillance.nightfall.data.http

// spec: Tests d'acceptation TA-06, TA-07
// spec: section "RetrofitClient + AuthInterceptor"
// RED by construction: fr.datasaillance.nightfall.data.http.AuthInterceptor does not exist yet

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.data.http.AuthInterceptor
// fr.datasaillance.nightfall.data.auth.TokenDataStore

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mockTokenDataStore: fr.datasaillance.nightfall.data.auth.TokenDataStore

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        // spec: D6 — AuthInterceptor injecté par Hilt ; ici testé via mock TokenDataStore
        mockTokenDataStore = mock()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // spec: TA-06 — TokenDataStore.getToken() retourne "test-jwt-token"
    //               → requête OkHttp contient Authorization: Bearer test-jwt-token
    @Test
    fun authInterceptor_addsBearer() {
        val token = "test-jwt-token"
        whenever(mockTokenDataStore.getToken()).thenReturn(token)

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val interceptor = AuthInterceptor(mockTokenDataStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()

        // spec: TA-06 — header Authorization: Bearer <token>
        assert(recordedRequest.getHeader("Authorization") == "Bearer $token") {
            "AuthInterceptor must add Authorization: Bearer <token> header — spec: TA-06"
        }
    }

    // spec: TA-07 — TokenDataStore.getToken() retourne null
    //               → la requête ne contient PAS le header Authorization
    @Test
    fun authInterceptor_noToken_noHeader() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val interceptor = AuthInterceptor(mockTokenDataStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()

        // spec: TA-07 — l'intercepteur ne forge pas de header vide quand pas de token
        assert(recordedRequest.getHeader("Authorization") == null) {
            "AuthInterceptor must NOT add Authorization header when token is null — spec: TA-07"
        }
    }

    // spec: TA-06 (edge case) — token présent sur plusieurs requêtes consécutives
    @Test
    fun authInterceptor_addsBearer_onEveryRequest() {
        val token = "persistent-jwt"
        whenever(mockTokenDataStore.getToken()).thenReturn(token)

        repeat(3) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200))
        }

        val interceptor = AuthInterceptor(mockTokenDataStore)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        repeat(3) {
            val request = Request.Builder()
                .url(mockWebServer.url("/endpoint$it"))
                .build()
            client.newCall(request).execute()
        }

        // spec: D6 — intercepteur unique injecte Bearer token sur toutes les requêtes
        repeat(3) {
            val recorded = mockWebServer.takeRequest()
            assert(recorded.getHeader("Authorization") == "Bearer $token") {
                "AuthInterceptor must inject Bearer token on every request — spec: D6"
            }
        }
    }
}

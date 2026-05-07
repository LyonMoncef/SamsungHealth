package fr.datasaillance.nightfall.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.AuthInterceptor
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.network.BackendUrlStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    fun provideAuthInterceptor(tokenDataStore: TokenDataStore): AuthInterceptor =
        AuthInterceptor(tokenDataStore)

    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    fun provideRetrofit(okHttpClient: OkHttpClient, backendUrlStore: BackendUrlStore): Retrofit =
        Retrofit.Builder()
            .baseUrl(backendUrlStore.getUrl().trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun provideNightfallApi(retrofit: Retrofit): NightfallApi =
        retrofit.create(NightfallApi::class.java)
}

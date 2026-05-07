package fr.datasaillance.nightfall.di

import android.content.Context
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.settings.SettingsDataStore
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel

object AppModule {
    fun provideTokenDataStore(context: Context): TokenDataStore =
        TokenDataStore(context)

    fun provideSettingsDataStore(context: Context): SettingsDataStore =
        SettingsDataStore(context)

    fun provideAuthViewModel(api: NightfallApi, tokenDataStore: TokenDataStore): AuthViewModel =
        AuthViewModel(api, tokenDataStore)
}

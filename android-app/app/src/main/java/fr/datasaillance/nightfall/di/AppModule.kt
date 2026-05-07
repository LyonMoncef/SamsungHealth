package fr.datasaillance.nightfall.di

import android.content.Context
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.settings.SettingsDataStore

object AppModule {
    fun provideTokenDataStore(context: Context): TokenDataStore =
        TokenDataStore(context)

    fun provideSettingsDataStore(context: Context): SettingsDataStore =
        SettingsDataStore(context)
}

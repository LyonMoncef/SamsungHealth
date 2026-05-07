package fr.datasaillance.nightfall.di

import android.content.Context
import fr.datasaillance.nightfall.data.auth.TokenDataStore

object AppModule {
    fun provideTokenDataStore(context: Context): TokenDataStore =
        TokenDataStore(context)
}

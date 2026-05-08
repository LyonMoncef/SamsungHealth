---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/MainActivity.kt
git_blob: c3823caef11e9ef2747cebc9965f96678700e771
last_synced: '2026-05-08T06:09:46Z'
loc: 48
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/MainActivity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/MainActivity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/MainActivity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.network.BackendUrlStore
import fr.datasaillance.nightfall.data.sleep.SleepRepositoryImpl
import fr.datasaillance.nightfall.di.NetworkModule
import fr.datasaillance.nightfall.ui.navigation.NavGraph
import fr.datasaillance.nightfall.ui.theme.NightfallTheme
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel

class MainActivity : ComponentActivity() {

    private val tokenDataStore by lazy { TokenDataStore(this) }
    private val backendUrlStore by lazy { BackendUrlStore(this) }

    private val api: NightfallApi by lazy {
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenDataStore)
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient, backendUrlStore)
        NetworkModule.provideNightfallApi(retrofit)
    }

    private val sleepViewModel: SleepViewModel by lazy {
        SleepViewModel(SleepRepositoryImpl(api, tokenDataStore))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightfallTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    hasToken      = tokenDataStore.hasToken(),
                    api           = api,
                    sleepViewModel = sleepViewModel,
                    backendUrl    = backendUrlStore.getUrl(),
                    onSaveUrl     = { url -> backendUrlStore.saveUrl(url) }
                )
            }
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `MainActivity` (class) — lines 16-48
- `onCreate` (function) — lines 32-47

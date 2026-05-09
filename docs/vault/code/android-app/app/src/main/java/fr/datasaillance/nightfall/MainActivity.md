---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/MainActivity.kt
git_blob: 639be917c40feb46c9c2d5e312034cc89b127495
last_synced: '2026-05-09T02:10:36Z'
loc: 40
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
import fr.datasaillance.nightfall.data.network.BackendUrlStore
import fr.datasaillance.nightfall.di.NetworkModule
import fr.datasaillance.nightfall.ui.navigation.NavGraph
import fr.datasaillance.nightfall.ui.theme.NightfallTheme

class MainActivity : ComponentActivity() {

    private val tokenDataStore by lazy { TokenDataStore(this) }
    private val backendUrlStore by lazy { BackendUrlStore(this) }
    private val api by lazy {
        val interceptor = NetworkModule.provideAuthInterceptor(tokenDataStore)
        val client      = NetworkModule.provideOkHttpClient(interceptor)
        val retrofit    = NetworkModule.provideRetrofit(client, backendUrlStore)
        NetworkModule.provideNightfallApi(retrofit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightfallTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController  = navController,
                    hasToken       = tokenDataStore.hasToken(),
                    backendUrl     = backendUrlStore.getUrl(),
                    onSaveUrl      = { url -> backendUrlStore.saveUrl(url) },
                    api            = api,
                    tokenDataStore = tokenDataStore,
                )
            }
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `MainActivity` (class) — lines 13-40
- `onCreate` (function) — lines 24-39

---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/MainActivity.kt
git_blob: 6c2c466bde2ce1f5bc53be85dabe0b18378e644e
last_synced: '2026-05-09T03:55:38Z'
loc: 52
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.network.BackendUrlStore
import fr.datasaillance.nightfall.di.NetworkModule
import fr.datasaillance.nightfall.ui.navigation.NavGraph
import fr.datasaillance.nightfall.ui.theme.NightfallTheme

class MainActivity : ComponentActivity() {

    private val tokenDataStore by lazy { TokenDataStore(this) }
    private val backendUrlStore by lazy { BackendUrlStore(this) }

    private fun buildApi(): NightfallApi {
        val interceptor = NetworkModule.provideAuthInterceptor(tokenDataStore)
        val client      = NetworkModule.provideOkHttpClient(interceptor)
        val retrofit    = NetworkModule.provideRetrofit(client, backendUrlStore)
        return NetworkModule.provideNightfallApi(retrofit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightfallTheme {
                var api by remember { mutableStateOf(buildApi()) }
                var backendUrl by remember { mutableStateOf(backendUrlStore.getUrl()) }
                val navController = rememberNavController()
                NavGraph(
                    navController  = navController,
                    hasToken       = tokenDataStore.hasToken(),
                    backendUrl     = backendUrl,
                    onSaveUrl      = { url ->
                        backendUrlStore.saveUrl(url)
                        backendUrl = url
                        api = buildApi()
                    },
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
- `MainActivity` (class) — lines 18-52
- `buildApi` (function) — lines 23-28
- `onCreate` (function) — lines 30-51

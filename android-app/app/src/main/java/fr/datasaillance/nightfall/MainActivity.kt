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

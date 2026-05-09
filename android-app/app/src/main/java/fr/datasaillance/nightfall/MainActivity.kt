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

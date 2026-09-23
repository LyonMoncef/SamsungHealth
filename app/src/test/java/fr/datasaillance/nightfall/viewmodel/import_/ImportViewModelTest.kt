package fr.datasaillance.nightfall.viewmodel.import_

import android.content.ContentResolver
import android.net.Uri
import fr.datasaillance.nightfall.domain.import_.ImportUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

// Depuis la Phase 1.2, l'écran d'import ne sert plus qu'à Google Takeout (lieux) :
// le sommeil et les pas viennent de Health Connect.
@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(TestCoroutineScheduler()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `l'etat initial est Idle`() {
        assertEquals(ImportUiState.Idle, ImportViewModel().uiState.value)
    }

    @Test
    fun `sans service Takeout, l'import echoue proprement avec un message`() {
        val viewModel = ImportViewModel(locationService = null)

        viewModel.startLocationImport(mock<ContentResolver>(), mock<Uri>())

        assertEquals(ImportUiState.LocationError("Service Timeline indisponible"), viewModel.uiState.value)
    }

    @Test
    fun `reset ramene l'ecran a l'etat Idle`() {
        val viewModel = ImportViewModel(locationService = null)
        viewModel.startLocationImport(mock<ContentResolver>(), mock<Uri>())

        viewModel.reset()

        assertEquals(ImportUiState.Idle, viewModel.uiState.value)
    }
}

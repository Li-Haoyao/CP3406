package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import au.edu.jcu.assessment.utilityapp.data.LanguageSettings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.ui.RadarViewModel
import au.edu.jcu.assessment.utilityapp.ui.UtilityApp
import au.edu.jcu.assessment.utilityapp.ui.theme.RadarTheme

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        LanguageSettings.restoreBeforeActivity(newBase)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: RadarViewModel = viewModel(factory = RadarViewModel.Factory)
            val state by model.uiState.collectAsStateWithLifecycle()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !state.preferences.darkMode
                    isAppearanceLightNavigationBars = !state.preferences.darkMode
                }
            }
            RadarTheme(dark = state.preferences.darkMode) { UtilityApp(state, model) }
        }
    }
}

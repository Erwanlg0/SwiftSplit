package com.elg.speedruncompanion

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.elg.speedruncompanion.application.port.input.ImportRunUseCase
import com.elg.speedruncompanion.application.port.output.SettingsPort
import com.elg.speedruncompanion.navigation.AppNavigation
import com.elg.speedruncompanion.ui.theme.SpeedrunTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var importRunUseCase: ImportRunUseCase

    @Inject
    lateinit var settingsPort: SettingsPort

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsPort.observeThemeMode().collectAsStateWithLifecycle(initialValue = "system")
            val language by settingsPort.observeLanguage().collectAsStateWithLifecycle(initialValue = null)

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // Gestion de la langue via l'API officielle Android/AppCompat
            // Cela permet de changer la langue dynamiquement sans casser le contexte Hilt (ViewModel)
            LaunchedEffect(language) {
                val lang = language ?: return@LaunchedEffect
                val appLocales = if (lang == "auto") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(lang)
                }
                if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }

            SpeedrunTheme(darkTheme = darkTheme) {
                SpeedrunMainScreen()
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        lifecycleScope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    importRunUseCase(bytes)
                        .onSuccess { run ->
                            Toast.makeText(
                                this@MainActivity,
                                "Imported: ${run.gameInfo.gameName} - ${run.gameInfo.categoryName}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to parse .lss: ${error.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error importing file: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
private fun SpeedrunMainScreen() {
    val navController = rememberNavController()

    AppNavigation(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

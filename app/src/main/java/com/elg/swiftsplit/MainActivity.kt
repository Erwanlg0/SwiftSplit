package com.elg.swiftsplit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.elg.swiftsplit.application.port.input.ImportRunUseCase
import com.elg.swiftsplit.application.port.input.SplitUseCase
import com.elg.swiftsplit.application.port.input.UndoSplitUseCase
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.navigation.AppNavigation
import com.elg.swiftsplit.ui.theme.SwiftSplitTheme
import android.view.KeyEvent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var importRunUseCase: ImportRunUseCase

    @Inject
    lateinit var splitUseCase: SplitUseCase

    @Inject
    lateinit var undoSplitUseCase: UndoSplitUseCase

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

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as android.app.Activity).window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
            }

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

            SwiftSplitTheme(darkTheme = darkTheme) {
                SwiftSplitMainScreen()
            }
        }
        handleIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            lifecycleScope.launch {
                if (settingsPort.observeGlobalHotkeysEnabled().first()) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        splitUseCase()
                    } else {
                        undoSplitUseCase()
                    }
                }
            }
            return true 
        }
        
        return super.onKeyDown(keyCode, event)
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
                                getString(R.string.main_import_success, run.gameInfo.gameName, run.gameInfo.categoryName),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.main_import_parse_failed, error.localizedMessage),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.main_import_error, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
private fun SwiftSplitMainScreen() {
    val navController = rememberNavController()

    AppNavigation(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

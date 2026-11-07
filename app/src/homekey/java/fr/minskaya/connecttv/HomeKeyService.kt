package fr.minskaya.connecttv

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Renvoie la touche Home vers Projectivy.
 *
 * Le launcher SFR déclare son filtre HOME avec une priorité 2 : Android le choisit
 * toujours, quel que soit le launcher par défaut enregistré. L'option « Remplacer le
 * launcher actuel » de Projectivy contourne ça en fermant toute fenêtre SFR, ce qui
 * ferme aussi les sections ouvertes par les tuiles. Ce service ne regarde que la touche.
 */
class HomeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyEvent ${KeyEvent.keyCodeToString(event.keyCode)} action=${event.action}")
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        // On consomme DOWN et UP ; on n'agit qu'au relâchement, une seule fois.
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
            return launchTarget()
        }
        return true
    }

    private fun launchTarget(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(TARGET_PACKAGE, TARGET_ACTIVITY))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            startActivity(intent)
            true
        } catch (e: Exception) {
            // Projectivy absent : on laisse passer, le système gère Home normalement.
            Log.w(TAG, "Impossible d'ouvrir $TARGET_PACKAGE", e)
            false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "HomeKeyService"
        const val TARGET_PACKAGE = "com.spocky.projengmenu"
        const val TARGET_ACTIVITY = "com.spocky.projengmenu.ui.home.MainActivity"
    }
}

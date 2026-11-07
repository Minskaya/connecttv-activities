package fr.minskaya.connecttv

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Activité "tremplin" : lance une section du launcher SFR (Connect TV) puis se ferme.
 * Actions acceptées par LauncherActivity : com.sfr.androidtv.TV / TVGUIDE / REPLAY / VOD / SEARCH.
 */
abstract class TrampolineActivity(private val action: String) : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(action)
            .setClassName(SFR_PACKAGE, SFR_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Launcher SFR introuvable", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    companion object {
        const val SFR_PACKAGE = "com.sfr.androidtv.launcher"
        const val SFR_ACTIVITY = "com.altice.androidtv.launcher.ui.LauncherActivity"
    }
}

class GuideActivity : TrampolineActivity("com.sfr.androidtv.TVGUIDE")
class TvActivity : TrampolineActivity("com.sfr.androidtv.TV")
class ReplayActivity : TrampolineActivity("com.sfr.androidtv.REPLAY")

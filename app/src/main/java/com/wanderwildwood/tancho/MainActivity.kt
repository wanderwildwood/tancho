/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modifications by woheller69
// Screen rewritten in Compose for the Kompakt's e-ink panel

package com.wanderwildwood.tancho

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.mudita.mmd.ThemeMMD

class MainActivity : BaseActivity() {

  private lateinit var soundClassifier: SoundClassifier
  private val state = ListeningState()

  /**
   * Read afresh on every resume rather than once: it is changed on the settings screen,
   * and coming back from there does not rebuild this one.
   */
  private var photoWhileListening by mutableStateOf(false)

  /**
   * The permission the app is about to ask for, with its reason shown first. Null when
   * nothing is being asked. The microphone comes before the location, one at a time: a
   * person told why, and asked for one thing, can say yes to one and no to the other.
   */
  private enum class Ask { MICROPHONE, LOCATION }

  private var asking by mutableStateOf<Ask?>(null)

  /**
   * BaseActivity sets the theme in onCreate, which replaces whatever the manifest asked
   * for. This screen draws its own bar with MMD, so letting it be given the theme with an
   * ActionBar puts the app's name on the screen twice, once in each bar.
   */
  override fun applyTheme() {
    setTheme(R.style.AppTheme_NoActionBar)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val storedInfluence = sharedPref.getFloat("meta_model_influence", 60.0f) / 100.0f
    state.metaInfluence.value = storedInfluence

    Thread {
      // "Cleared after closing" is done on the way in rather than on the way out. An app
      // is not always given a chance to run anything when it goes: the process can simply
      // be killed. Clearing at the start of the next session is the same promise kept in
      // the one place it can actually be relied on.
      if (sharedPref.getBoolean("clear_recordings", false)) {
        val gone = WavUtils.clearRecordings(this)
        if (gone > 0) Log.i("MainActivity", "Cleared $gone recordings from the last session")
      } else {
        // And whatever an earlier deletion left behind.
        val gone = WavUtils.sweepOrphans(this, BirdDBHelper.getInstance(this).allTimestamps())
        if (gone > 0) Log.i("MainActivity", "Removed $gone recordings no longer in the log")
      }
    }.start()

    soundClassifier = SoundClassifier(this, state, SoundClassifier.Options())

    setContent {
      ThemeMMD(colorScheme = monochrome) {
       // A blinking cursor repaints its field twice a second on a panel that cannot afford it.
       CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
        val isListening by state.isListening.collectAsStateWithLifecycle()
        val micAllowed by state.micAllowed.collectAsStateWithLifecycle()
        val heard by state.heard.collectAsStateWithLifecycle()
        val photo by state.photo.collectAsStateWithLifecycle()
        var placeAndDate by remember { mutableStateOf(PlaceAndDate.nearest(storedInfluence)) }
        // Re-read whenever the screen comes back, because a fix can arrive while it is up.
        val placeKnown by state.placeKnown.collectAsStateWithLifecycle()
        val settings = remember { Settings(this@MainActivity) }

        ListeningScreen(
          isListening = isListening,
          micAllowed = micAllowed,
          onAllowMicrophone = { allowMicrophone() },
          placeKnown = placeKnown,
          settings = settings,
          // Re-reads the preference and tells the screen what it now knows, so turning GPS
          // back on stops saying "Place not known" without leaving the screen.
          onPlaceChanged = {
            // Switching GPS on is the moment to ask for the permission it needs, with the
            // reason first, if it was not given before.
            if (!settings.manualLocation && !checkLocationPermission()) asking = Ask.LOCATION
            LocationHelper.requestLocation(this@MainActivity, soundClassifier)
          },
          heard = heard,
          placeAndDate = placeAndDate,
          showPhoto = photoWhileListening,
          photoAssetId = photo?.assetId,
          onToggleListening = { setListening(!isListening) },
          onCyclePlaceAndDate = {
            placeAndDate = placeAndDate.next()
            state.metaInfluence.value = placeAndDate.influence
            sharedPref.edit()
              .putFloat("meta_model_influence", placeAndDate.influence * 100f)
              .apply()
          },
          onOpenHeard = { openInLog(it) },
        )
        PermissionAsk()
       }
      }
    }

    state.setMicAllowed(checkMicrophonePermission())
    askForPermissions()
  }

  /** The reason for the permission about to be asked, and the choice to go on or not. */
  @androidx.compose.runtime.Composable
  private fun PermissionAsk() {
    val ask = asking ?: return
    val title = if (ask == Ask.MICROPHONE) R.string.ask_microphone_title else R.string.ask_location_title
    val body = if (ask == Ask.MICROPHONE) R.string.ask_microphone_body else R.string.ask_location_body
    EInkDialog(onDismiss = { declined(ask) }) {
      TextMMD(
        text = stringResource(title),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(12.dp))
      TextMMD(text = stringResource(body), style = MaterialTheme.typography.bodyMedium)
      Spacer(modifier = Modifier.height(18.dp))
      OutlinedButtonMMD(onClick = { accepted(ask) }, modifier = Modifier.fillMaxWidth()) {
        TextMMD(stringResource(R.string.ask_continue))
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButtonMMD(onClick = { declined(ask) }, modifier = Modifier.fillMaxWidth()) {
        TextMMD(stringResource(R.string.ask_not_now))
      }
    }
  }

  private fun accepted(ask: Ask) {
    asking = null
    when (ask) {
      Ask.MICROPHONE -> {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPref.getBoolean("bluetooth", false)
          && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
          && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        requestPermissions(perms.toTypedArray(), REQUEST_MICROPHONE)
      }
      Ask.LOCATION -> requestPermissions(
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
        REQUEST_LOCATION,
      )
    }
  }

  /** "Not now" for the microphone still goes on to ask about the place: they are separate. */
  private fun declined(ask: Ask) {
    asking = null
    if (ask == Ask.LOCATION) {
      val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
      sharedPref.edit().putInt("location_declined", sharedPref.getInt("location_declined", 0) + 1).apply()
    }
    if (ask == Ask.MICROPHONE) askForLocation()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      REQUEST_MICROPHONE -> {
        val allowed = checkMicrophonePermission()
        state.setMicAllowed(allowed)
        // Granted while the screen says it is listening: start hearing at once, rather than
        // leaving a word that says Listening over a recogniser nobody started.
        if (allowed && state.isListening.value) {
          soundClassifier.isPaused = false
          soundClassifier.start()
        }
        askForLocation()
      }
      REQUEST_LOCATION -> {
        if (checkLocationPermission()) {
          LocationHelper.requestLocation(this, soundClassifier)
        } else {
          val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
          sharedPref.edit().putInt("location_declined", sharedPref.getInt("location_declined", 0) + 1).apply()
        }
      }
    }
  }

  /**
   * The chip's answer to "No microphone". Android stops showing its own question after two
   * refusals, so after one this opens the app's page in the phone's settings instead, where
   * the permission can be switched on by hand.
   */
  private fun allowMicrophone() {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val askedBefore = sharedPref.getBoolean("microphone_asked", false)
    if (!askedBefore || shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
      sharedPref.edit().putBoolean("microphone_asked", true).apply()
      requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE)
    } else {
      Toast.makeText(this, getString(R.string.open_app_settings), Toast.LENGTH_LONG).show()
      runCatching {
        startActivity(
          Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
      }
    }
  }

  /**
   * Stopping stops the recorder, rather than leaving it running and throwing the audio
   * away. A screen that says it is not listening should be telling the truth.
   */
  private fun setListening(listening: Boolean) {
    state.flush()
    state.setListening(listening)
    if (listening) {
      if (checkMicrophonePermission()) soundClassifier.isPaused = false
    } else {
      soundClassifier.isPaused = true
    }
  }

  /**
   * Opens the log at this hearing. The recorder is not touched here: leaving the screen
   * pauses it as it always has, and Back resumes it if it was running, so the hearing
   * goes on where it left off.
   */
  private fun openInLog(entry: Heard) {
    // A bird still singing may have a later reading held back from the screen; fold it in
    // so the log is searched over the whole of the hearing.
    state.flush()
    val current = state.heard.value.firstOrNull { it.firstHeard == entry.firstHeard } ?: entry
    startActivity(
      Intent(this, ViewActivity::class.java)
        .putExtra(ViewActivity.EXTRA_SPECIES, current.speciesIndex)
        .putExtra(ViewActivity.EXTRA_FROM, current.firstHeard)
        .putExtra(ViewActivity.EXTRA_TO, current.lastHeard)
        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    )
  }

  override fun onResume() {
    super.onResume()
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    photoWhileListening = sharedPref.getBoolean("show_images", false)
    if (sharedPref.getBoolean("bluetooth", false)){
      audioManager.startBluetoothSco()
      audioManager.isBluetoothScoOn = true
    } else {
      audioManager.stopBluetoothSco()
      audioManager.isBluetoothScoOn = false
    }

    // Coming back from settings, where the bird names may have changed language.
    soundClassifier.refreshLabels()

    LocationHelper.requestLocation(this, soundClassifier)
    // The screen says so (No microphone, Place not known), where a toast came and went.
    // The permission may have been switched on in the phone's settings meanwhile.
    val micAllowed = checkMicrophonePermission()
    state.setMicAllowed(micAllowed)
    if (micAllowed && state.isListening.value) soundClassifier.start()
    keepScreenOn(true)
  }

  override fun onPause() {
    super.onPause()
    state.flush()
    LocationHelper.stopLocation(this)
    if (soundClassifier.isRecording) soundClassifier.stop()
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.stopBluetoothSco()
    audioManager.isBluetoothScoOn = false
  }

  private fun checkMicrophonePermission(): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO ) == PackageManager.PERMISSION_GRANTED) {
      return true
    } else {
      return false
    }
  }

  private fun checkLocationPermission(): Boolean {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    if (sharedPref.getBoolean("manual_location", false) || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      return true
    } else {
      return false
    }
  }

  /** First start of a session: the microphone, then (separately) the place. */
  private fun askForPermissions() {
    if (!checkMicrophonePermission()) {
      PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("microphone_asked", true).apply()
      asking = Ask.MICROPHONE
    } else {
      askForLocation()
    }
  }

  /**
   * Only when the place is neither typed in nor already allowed, and not once the question
   * has been turned down twice: asking at every start of the app is nagging. Switching GPS
   * on from the place chip asks again, because then it is the person's own request.
   */
  private fun askForLocation() {
    if (checkLocationPermission()) return
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    if (sharedPref.getInt("location_declined", 0) >= 2) return
    asking = Ask.LOCATION
  }

  private fun keepScreenOn(enable: Boolean) =
    if (enable) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

  companion object {
    private const val REQUEST_MICROPHONE = 1337
    private const val REQUEST_LOCATION = 1338
  }
}

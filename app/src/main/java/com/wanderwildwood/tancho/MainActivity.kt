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
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
      }
    }.start()

    soundClassifier = SoundClassifier(this, state, SoundClassifier.Options())

    setContent {
      ThemeMMD {
        val isListening by state.isListening.collectAsStateWithLifecycle()
        val heard by state.heard.collectAsStateWithLifecycle()
        val photo by state.photo.collectAsStateWithLifecycle()
        var placeAndDate by remember { mutableStateOf(PlaceAndDate.nearest(storedInfluence)) }
        // Re-read whenever the screen comes back, because a fix can arrive while it is up.
        val placeKnown by state.placeKnown.collectAsStateWithLifecycle()
        val settings = remember { Settings(this@MainActivity) }

        ListeningScreen(
          isListening = isListening,
          placeKnown = placeKnown,
          settings = settings,
          // Re-reads the preference and tells the screen what it now knows, so turning GPS
          // back on stops saying "Place not known" without leaving the screen.
          onPlaceChanged = { LocationHelper.requestLocation(this@MainActivity, soundClassifier) },
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
        )
      }
    }

    requestPermissions()
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
    if (!checkLocationPermission()){
      Toast.makeText(this, this.resources.getString(R.string.error_location_permission), Toast.LENGTH_SHORT).show()
    }
    if (checkMicrophonePermission()){
      if (state.isListening.value) soundClassifier.start()
    } else {
      Toast.makeText(this, this.resources.getString(R.string.error_audio_permission), Toast.LENGTH_SHORT).show()
    }
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

  private fun requestPermissions() {
    val perms = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      perms.add(Manifest.permission.RECORD_AUDIO)
    }

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

    if (!sharedPref.getBoolean("manual_location", false)
      && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (sharedPref.getBoolean("bluetooth", false)
      && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
      && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        perms.add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    if (!perms.isEmpty()) requestPermissions(perms.toTypedArray(), REQUEST_PERMISSIONS)
  }

  private fun keepScreenOn(enable: Boolean) =
    if (enable) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

  companion object {
    private const val REQUEST_PERMISSIONS = 1337
  }
}

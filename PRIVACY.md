# Privacy

Recognition happens on the phone. No recording, no detection and no location is ever sent
anywhere by this app.

The app does reach the network in three places, all of them listed below, and none of them
carry what you heard or where you were. The rest of this page is the evidence, because a
privacy policy that cannot be checked is just a promise.

## What is recorded, and what happens to it

The microphone is open while the app is listening. Audio goes into a buffer, the BirdNET
model runs on it in `SoundClassifier.kt`, and the buffer is overwritten by the next three
seconds. Nothing is uploaded, because there is no code that could upload it.

Audio is written to disk only if you turn on **Save .wav files** in settings, which is off
by default. Those files go to the app's own folder on external storage, not into your music.
A morning outdoors can write hundreds of megabytes — the recogniser runs every 800ms and every
detection above the threshold writes a file — and a music library is the wrong place for that:
every player, scanner and backup would find them and offer them as songs.

Being the app's own folder has a consequence worth stating plainly: **uninstalling takes the
recordings with it**, and the "keep recordings for one session only" setting deletes them too.
Copy anything you want to keep somewhere else first.

## Location

Only if you grant it. It is used to ask the second, much smaller model whether a given bird
can plausibly be where you are in the week you are in, which is what stops the app offering
you a species from another continent.

- It comes from the GPS receiver directly (`LocationManager.GPS_PROVIDER` in
  `LocationHelper.java`). There is no fused-location provider and no network lookup, so
  asking for your position does not tell anyone that you asked.
- The coordinates are stored with each detection in the app's own database, so a saved
  observation says where it was made. That database is in the app's private storage.
- You can turn the location influence down to nothing with the slider on the main screen,
  or set a location by hand in settings and never grant the permission at all.

## Network

1. **The model, once.** On first run the app downloads the BirdNET model files (~39MB, or
   ~63MB for 32-bit) from `raw.githubusercontent.com`. After that it identifies birds with
   no network at all — aeroplane mode included.
2. **Bird photographs, when shown.** Images come from `macaulaylibrary.org` in a WebView.
   On the listening screen this is off by default (**Show Images** in settings). In the
   observation and species lists, tapping an entry loads its photograph. Cornell's server
   therefore learns your IP address and which species you looked at, the same as opening
   the page in a browser would. Trackers and page scripts are blocked before they load
   (`MlWebViewClient.kt`); no cookies, accounts or identifiers are sent by the app.
3. **eBird, only if you tap it.** The species link opens `ebird.org` in your own browser.

There is no analytics, no crash reporting and no update check. The HTTP library the app
carried but never called has been removed.

## What is stored, and where

| | |
|---|---|
| `databases/BirdDatabase.db` | Your detections: time, species, probability, and coordinates if you granted location. App-private. |
| `files/` | The downloaded model files. App-private. |
| Shared preferences | Your settings. App-private. |
| `files/Music/*.wav` | Recordings, only if you turned that on. App-private: removed on uninstall, and by the one-session setting. |

App-private means other apps cannot read it and it goes away when you uninstall. The
database can be exported or deleted from inside the app, in the observations list.

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | To hear birds. This is the app. |
| `INTERNET` | The three uses above. |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | The location model. Optional. |
| `BLUETOOTH_CONNECT` | To record from a Bluetooth microphone if you select one. |
| `MODIFY_AUDIO_SETTINGS` | To choose the audio input the recogniser reads. |

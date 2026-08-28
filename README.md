# 探鳥 tanchō — Birding

Identifies birds by their song, on the [Mudita Kompakt](https://mudita.com/products/kompakt/).
Listens through the microphone and names what it hears, offline.

*Tanchō* is 探鳥 — bird-seeking, the ordinary word for going out to look for birds; 探鳥会 is a
birdwatching outing. This is the seeking half of it: you stop walking, hold still, and it tells you
what is singing.

Fork of [whoBIRD](https://github.com/woheller69/whoBIRD) by woheller69.

## How it works

Recognition is [BirdNET](https://github.com/kahst/BirdNET-Analyzer), running on the phone.
Nothing is sent away to be identified. The model file is about 39MB and is downloaded once,
on first run; after that the app works with no network at all.

A second, much smaller model narrows the answer by where you are and what week of the year
it is, so a bird that cannot be here now is not offered. That is what the location permission
is for, and it can be turned off in settings.

Detections are kept in a database on the phone, viewable, exportable and deletable from the
app. What the app records, stores and sends is set out in [PRIVACY.md](PRIVACY.md).

## Building

    ./gradlew assembleRelease

Release builds are signed with a keystore in `signing/`, which is gitignored. There is no
fallback: without it `assembleRelease` produces an *unsigned* APK, which will not install
anywhere. That is deliberate — a signing key committed to a public repo is not a signing key,
it is a formality, and a missing one should stop you rather than quietly hand you something
installable. (`assembleDebug` still works, signed with the usual Android debug key.)

## Getting it, and keeping it

Download <https://github.com/wanderwildwood/tancho/releases/latest/download/tancho.apk> and
sideload it. That address always points at the newest release, and every release publishes a
`.sha256` beside the APK if you would rather check than trust.

For updates without doing this by hand, add this repository to
[Obtainium](https://github.com/ImranR98/Obtainium):

    https://github.com/wanderwildwood/tancho

It will offer each new release as it appears. **The application id is settled** — updates
install over what you have, keeping your settings and anything the app has stored.

## License

GPLv3. Upstream whoBIRD is © woheller69; the parts of this app that came from there are still
under that licence, and the rest is too.

- Built on the [BirdNET framework](https://github.com/kahst/BirdNET-Analyzer) by
  [@kahst](https://github.com/kahst), published under CC BY-NC-SA 4.0.
- On first run it downloads the BirdNET TFLite models from
  [whoBIRD-TFlite](https://github.com/woheller69/whoBIRD-TFlite), also CC BY-NC-SA 4.0.
  **No commercial use.**
- Label files from BirdNET are used under GPLv3, with
  [permission from the author](https://github.com/woheller69/whoBIRD/issues/1).
- Uses code from the [TensorFlow Lite examples](https://www.tensorflow.org/lite/examples),
  Apache 2.0.
- Uses [Zip4j](https://github.com/srikanth-lingala/zip4j), Apache 2.0.
- Uses [iirj](https://github.com/berndporr/iirj), Apache 2.0.
- The launcher icon is from a CC0 photograph; see `misc/`.

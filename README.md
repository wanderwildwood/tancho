# eInk Birding

Identifies birds by their song, on the [Mudita Kompakt](https://mudita.com/products/kompakt/).
Listens through the microphone and names what it hears, offline.

Fork of [whoBIRD](https://github.com/woheller69/whoBIRD) by woheller69.

## How it works

Recognition is [BirdNET](https://github.com/kahst/BirdNET-Analyzer), running on the phone.
Nothing is sent away to be identified. The model file is about 39MB and is downloaded once,
on first run; after that the app works with no network at all.

A second, much smaller model narrows the answer by where you are and what week of the year
it is, so a bird that cannot be here now is not offered. That is what the location permission
is for, and it can be turned off in settings.

Detections are kept in a database on the phone, viewable, exportable and deletable from the
app.

## Building

    ./gradlew assembleRelease

Release builds are signed with a key in `signing/`, which is not in this repository. Without
it the build falls back to the default debug key and still works.

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

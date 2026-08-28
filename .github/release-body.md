Identifies birds by their song on the Mudita Kompakt. Recognition is
[BirdNET](https://birdnet.cornell.edu/), running on the phone — nothing is sent away to
be identified.

## What it needs

The microphone, and nothing else. Give it a fixed location in Settings and it never asks
for location permission at all. See [PRIVACY.md](PRIVACY.md).

On first run it fetches a model of about 39MB. After that it identifies with no network.
A bird's photograph is fetched when you open its row, from the Macaulay Library's image
host; without a signal the rest of the app is unaffected.

## The download

`tancho.apk` and `tancho-<version>.apk` are the same file. The unversioned one
is there so a link to it keeps working after the next release. Verify either against the
`.sha256` beside it if you like.

Free software under the GPLv3, forked from [whoBIRD](https://github.com/woheller69/whoBird)
by woheller69. The BirdNET models are CC BY-NC-SA 4.0 — no commercial use.

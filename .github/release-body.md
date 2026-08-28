Identifies birds by their song on the Mudita Kompakt. Recognition is
[BirdNET](https://birdnet.cornell.edu/), running on the phone — nothing is sent away to
be identified.

**1.1.1** — the first screens a new installation shows, tidied. The Heard screen said
nothing at all when there was nothing in it, which reads as broken rather than as empty.
The download screen was the last one still carrying a bar with the app's name. And the
settings screen was two screens: three rows inherited from upstream were in Title Case
with their units in brackets after the label, and half the rows explained themselves while
the other half did not. Every row explains itself now, and the units sit with the numbers.

**1.1.0** — the bird photographs load. If you have 1.0.5 or earlier, please update.

The panel stayed blank because the phone would not let the app draw them, and that could
not be fixed from inside the app as it was written. A WebView paints in a separate
process, and MediaTek's DuraSpeed — which every Kompakt ships — had put this app on its
suppress list, so that process was never allowed to start. No callback ever fired and
nothing was logged. Reinstalling did not help, and neither did rebooting, because the
list is not the app's to clear.

There is no WebView now. The photograph is a plain image, fetched and decoded by the app
itself, so there is no second process to be refused. It is sharper for it, it is there
without a signal once you have seen it, and when it cannot be had you get the app's own
mark rather than an empty rectangle that never explains itself.

Also in this release: opening a row plays back what was heard, and can draw it — time
across, pitch up, loud dark. Recordings are 288KB each, so they are the app's own files
rather than additions to your music, and there is a setting to keep them for one session
only. The bars naming the app are gone from every screen and the lists are longer for it.
Export the log, save a backup and delete the log have moved into Settings, from a bar
that never drew them.

## What it needs

The microphone, and nothing else. Give it a fixed location in Settings and it never asks
for location permission at all. See [PRIVACY.md](PRIVACY.md).

On first run it fetches a model of about 39MB. After that it identifies with no network.
A bird's photograph is fetched when you open its row, from the Macaulay Library's image
host; without a signal the rest of the app is unaffected.

## The download

`eink-birding.apk` and `eink-birding-<version>.apk` are the same file. The unversioned one
is there so a link to it keeps working after the next release. Verify either against the
`.sha256` beside it if you like.

Free software under the GPLv3, forked from [whoBIRD](https://github.com/woheller69/whoBird)
by woheller69. The BirdNET models are CC BY-NC-SA 4.0 — no commercial use.

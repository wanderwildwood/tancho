#!/bin/sh
# Fetches the two BirdNET model files the app is built with, from one fixed commit of the
# whoBIRD-TFlite repository, and refuses them unless each is the exact file this repository was
# tested against. They are ~41 MB together, so they are not committed; run this once before
# building from a clean clone. The build checks the same hashes again (app/build.gradle).
#
# The models are BirdNET's (Stefan Kahl and the Cornell Lab of Ornithology), CC BY-NC-SA 4.0:
# https://github.com/woheller69/whoBIRD-TFlite
set -eu
cd "$(dirname "$0")"

BASE=https://raw.githubusercontent.com/woheller69/whoBIRD-TFlite/a79d101796654cf22f46383328547def828510e8

fetch() {
    name=$1; sha=$2
    if [ ! -f "$name" ]; then
        curl -fL --retry 3 -o "$name.part" "$BASE/$name"
        mv "$name.part" "$name"
    fi
    echo "$sha  $name" | sha256sum -c -
}

fetch BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite 5c64ba3ffab264ccbfafe784896392051a86be63b0cc507e7683db7df114546b
fetch BirdNET_GLOBAL_6K_V2.4_MData_Model_V2_FP16.tflite 1226f23fc20362617deb09178f366111b70cf085c2c82893f2814e5acedce6c2

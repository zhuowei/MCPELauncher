#!/bin/bash
set -e
cd ..
wget -O android-sdk.zip "https://dl.google.com/android/repository/tools_r25.2.5-linux.zip"
7z -oandroid-sdk-linux_86 x android-sdk.zip
yes | android-sdk-linux_86/tools/bin/sdkmanager \
"platform-tools" \
"platforms;android-29" \
"platforms;android-23" \
"platforms;android-21" \
"platforms;android-10" \
"platforms;android-9" \
"build-tools;27.0.3"
# sdkmanager doesn't have the old versions of these libraries:
wget -O google_play_services_8487000_r29.zip "https://dl-ssl.google.com/android/repository/google_play_services_8487000_r29.zip"
7z -oandroid-sdk-linux_86/extras/google x google_play_services_8487000_r29.zip
# TODO: cardview

wget -O android-ndk-r10c-linux-x86_64.bin "http://dl.google.com/android/ndk/android-ndk-r10c-linux-x86_64.bin"
dd if=android-ndk-r10c-linux-x86_64.bin bs=387396 skip=1 of=ndk.7z
7z x ndk.7z

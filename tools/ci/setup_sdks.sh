#!/bin/bash
set -e
rootdir="$PWD"
cd ..
wget_args="-nv"
wget $wget_args -O android-sdk.zip "https://dl.google.com/android/repository/tools_r25.2.5-linux.zip"
7z -oandroid-sdk-linux_86 x android-sdk.zip
yes | android-sdk-linux_86/tools/bin/sdkmanager \
"platform-tools" \
"platforms;android-29" \
"platforms;android-23" \
"platforms;android-21" \
"platforms;android-10" \
"platforms;android-9" \
"build-tools;27.0.3" \
"build-tools;23.0.3"
# sdkmanager doesn't have the old versions of these libraries:
wget $wget_args -O google_play_services_8487000_r29.zip "https://dl-ssl.google.com/android/repository/google_play_services_8487000_r29.zip"
7z -oandroid-sdk-linux_86/extras/google x google_play_services_8487000_r29.zip
android update lib-project -t android-23 -p android-sdk-linux_86/extras/google/google-play-services/libproject/google-play-services_lib

wget $wget_args -O support_r23.1.1.zip "https://dl-ssl.google.com/android/repository/support_r23.1.1.zip"
7z -oandroid-sdk-linux_86/extras/android x support_r23.1.1.zip
# Update cardview with an Ant project
cp "$rootdir/tools/ci/cardview_lib/"* android-sdk-linux_86/extras/android/support/v7/cardview/

wget $wget_args -O android-ndk-r10c-linux-x86_64.bin "http://dl.google.com/android/ndk/android-ndk-r10c-linux-x86_64.bin"
dd if=android-ndk-r10c-linux-x86_64.bin bs=387396 skip=1 of=ndk.7z
7z x ndk.7z
wget $wget_args -O apktool.jar "https://bitbucket.org/iBotPeaches/apktool/downloads/apktool_2.3.1.jar"
echo "java -jar $PWD/apktool.jar \"\$@\"" >apktool
chmod +x apktool
wget $wget_args -O dex2jar-0.0.9.8.tar.gz "https://downloads.sourceforge.net/project/dex2jar/dex2jar-0.0.9.8.tar.gz"
tar xf dex2jar-0.0.9.8.tar.gz
ln -s dex2jar-0.0.9.8/dex2jar.sh dex2jar

#!/bin/bash
set -e
rm -r external_libs jni/armeabi-v7a jni/x86 || true
mkdir external_libs jni/armeabi-v7a jni/x86
ln -s "$(readlink -e ../mcpe/mcpe180/outlib_interop)" external_libs/xbox_interop
ln -s "$ANDROID_HOME/extras/android/support/v7/cardview" external_libs/cardview
ln -s "$(readlink -e ../mcpe/1141/lib/armeabi-v7a/libminecraftpe.so)" jni/armeabi-v7a/libminecraftpe.so
ln -s "$(readlink -e ../mcpe/1141/lib/armeabi-v7a/libfmod.so)" jni/armeabi-v7a/libfmod.so
ln -s "$(readlink -e ../mcpe/190b3_x86/lib/x86/libminecraftpe.so)" jni/x86/libminecraftpe.so
ln -s "$(readlink -e ../mcpe/190b3_x86/lib/x86/libfmod.so)" jni/x86/libfmod.so
android update project -t android-21 -p httpclient
ln -s "$(readlink -e ../android-sdk-linux_86/extras/android/support/v4/android-support-v4.jar)" libs/


cd ../mcpelauncher-app
cp -r ../mcpe/1141/assets ./

# TODO(zhuowei): copy this back into mcpelauncher-app/mergeassets.sh

rm -r assets/structures assets/resource_packs/education assets/resource_packs/edu_loading_screens assets/resource_packs/vanilla_vr assets/resource_packs/vanilla/sounds || true
cp ../slickcraft/items/* assets/resource_packs/vanilla/textures/items/
cp ../slickcraft/blocks/* assets/resource_packs/vanilla/textures/blocks/
sed -i -e "s@../../../downloads/android-sdk-linux_86/extras/google/google_play_services@../android-sdk-linux_86/extras/google/google-play-services@" project.properties
cd ..
android update lib-project -t android-10 -p libsuperuser/libsuperuser
android update project -t android-29 -p mcpelauncher-app
android update lib-project -t android-23 -p MCPELauncher

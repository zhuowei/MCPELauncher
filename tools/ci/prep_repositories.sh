#!/bin/bash
set -e
rm -r external_libs jni/armeabi-v7a jni/x86 || true
mkdir external_libs jni/armeabi-v7a jni/x86
ln -s ../mcpe/mcpe180/outlib_interop external_libs/xbox_interop
ln -s "$ANDROID_HOME/extras/android/support/v7/cardview" external_libs/cardview
ln -s ../mcpe/1141/lib/armeabi-v7a/libminecraftpe.so jni/armeabi-v7a/libminecraftpe.so
ln -s ../mcpe/1141/lib/armeabi-v7a/libfmod.so jni/armeabi-v7a/libfmod.so
ln -s ../mcpe/190b3_x86/lib/x86/libminecraftpe.so jni/x86/libminecraftpe.so
ln -s ../mcpe/180b3_x86/lib/x86/libfmod.so jni/x86/libfmod.so


cd ../mcpelauncher-app
cp -r ../mcpe/1141/assets ./

# TODO(zhuowei): copy this back into mcpelauncher-app/mergeassets.sh

rm -r assets/structures assets/resource_packs/education assets/resource_packs/edu_loading_screens assets/resource_packs/vanilla_vr assets/resource_packs/vanilla/sounds
cp ../slickcraft/items/* assets/resource_packs/vanilla/textures/items/
cp ../slickcraft/blocks/* assets/resource_packs/vanilla/textures/blocks/

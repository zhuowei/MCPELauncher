# MCPELauncher: a custom Minecraft PE launcher for Android

MCPELauncher is a custom Minecraft PE launcher that wraps around Minecraft PE and provides texture pack and patch loading support.

Just choose Options on the main screen, then select a texture pack. It is that easy!

This program loads libminecraftpe.so from the currently installed copy of Minecraft PE, so it should be compatiable with the mod patching features of PocketTool and QuickPatch.

There is a 3-PTPatch limitation on this free version.

Known issues:
 - Sensitivity option in the option screen not implemented

Download from the Forums thread.

This program uses aFileChooser by Paul Burke. See https://github.com/zhuowei/aFileChooser for licensing information.

This program contains code from Snowbound\'s PTPatchTool, licensed under the Eclipse Public License.

The demonstration texture pack, RepetiCraft, is made by Theevilpplz and used with permission.

This program contains code from 2of1\'s Andhook framework; used with permission.

This program uses the libsuperuser library by Chainfire.

## Building

Building requires the Android SDK (it's built with the Gingerbread SDK currently; not sure if it works with other SDKs) as well as the Android NDK
to build the embedded copy of PokerFace and Andhook.

```

git clone git@github.com:zhuowei/aFileChooser.git
git clone git@github.com:zhuowei/MCPELauncher.git
git clone git@github.com:zhuowei/MCPELauncher-app.git
git clone https://github.com/Chainfire/libsuperuser.git
android update lib-project -p libsuperuser/libsuperuser -t 1
cd MCPELauncher
ndk-build
cd ..
cd MCPELauncher-app
ant clean debug

```

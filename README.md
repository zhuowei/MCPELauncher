# MCPELauncher: a custom Minecraft PE launcher for Android

MCPELauncher is a custom Minecraft PE launcher that wraps around Minecraft PE and provides texture pack and patch loading support.

Just choose Options on the main screen, then select a texture pack. It is that easy!

This program loads libminecraftpe.so from the currently installed copy of Minecraft PE, so it should be compatiable with the mod patching features of PocketTool and QuickPatch.

There is a 3-PTPatch limitation on this free version.

Known issues:
 - Sensitivity option in the option screen not implemented

Download from the Forums thread.

This program uses aFileChooser by Paul Burke. See https://github.com/zhuowei/aFileChooser for licensing information.

## Building

```

git clone git@github.com:zhuowei/aFileChooser.git
git clone git@github.com:zhuowei/Pokerface.git
git clone git@github.com:zhuowei/MCPELauncher.git
git clone git@github.com:zhuowei/MCPELauncher-app.git
git clone https://github.com/Chainfire/libsuperuser.git
android update lib-project -p libsuperuser/libsuperuser -t 1

cd MCPELauncher-app
ant clean debug

```

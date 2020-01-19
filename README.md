# BlockLauncher: a custom Minecraft launcher for Android

![](https://raw.githubusercontent.com/zhuowei/MCPELauncher/master/res/drawable-xxxhdpi/ic_launcher.png)
[![Build Status](https://github.com/zhuowei/MCPELauncher/workflows/Build%20BlockLauncher/badge.svg)](https://github.com/zhuowei/MCPELauncher/actions)

## Building

BlockLauncher's build system is complicated and out of date. It still uses Ant
(and thus is incompatible with Android Studio), and uses older versions of various libraries.

Scripts to setup a build environment can be found in `tools/ci/ci_build.sh`.

This script is tested to work on GitHub Actions' Ubuntu 18.04 image, and on a fresh Ubuntu 19.10 install.

You may need to install:

```
sudo apt install git p7zip-full openjdk-8-jdk ant
sudo update-java-alternatives -s java-1.8.0-openjdk-amd64
```

## License

Most of the code is licensed under the Apache License, version 2.0.

Some code is licensed under different licenses: see the _Third-party licenses_ section below.

## Third-party licenses

This program uses aFileChooser by Paul Burke. See https://github.com/zhuowei/aFileChooser for licensing information.

This program contains code from machinamentum\'s PTPatchTool, licensed under the Eclipse Public License.

The sample textures are based on NathanX's Slickcraft texture pack.

This program contains a version of Saurik's Cydia Substrate, licensed under the LGPL.

This program uses the libsuperuser library by Chainfire.

This program uses the Rhino JavaScript interpreter.

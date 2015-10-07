# BlockLauncher: a custom Minecraft PE launcher for Android

BlockLauncher is a custom Minecraft PE launcher that wraps around Minecraft PE and provides texture pack and patch loading support.

Just choose Options on the main screen, then select a texture pack. It is that easy!

This program loads libminecraftpe.so from the currently installed copy of Minecraft PE, so it should be compatible with the mod patching features of PocketTool and QuickPatch.

There is a 3-PTPatch limitation on this free version.

Download from the Forums thread.

This program uses aFileChooser by Paul Burke. See https://github.com/zhuowei/aFileChooser for licensing information.

This program contains code from Snowbound\'s PTPatchTool, licensed under the Eclipse Public License.

The demonstration texture pack, RepetiCraft, is made by Theevilpplz and used with permission.

This program contains a version of Saurik's Cydia Substrate: source at https://drive.google.com/file/d/0B50ApLKOyu8bX3VnRmYzNUcxT0U/edit?usp=sharing

This program uses the libsuperuser library by Chainfire.

This program uses the Rhino JavaScript interpreter.

## Building

1.  Download Android NDK and extract it somewhere with NO SPACES. Personally I would put it in:   C:/
2.  Download Android SDK and extract it somewhere with NO SPACES. Personally I would put it in:   C:/
     Also, open SDK Manager and install API 10 and API 17
3.  Download Apache ANT and extract it somewhere with NO SPACES. Personally I would put it in:    C:/
4.  Setup Environment variables by pressing START, right clicking on Computer, then clicking "Advanced system settings" on 		the left side
5.  Under "User variables" add "ANT_HOME" and the value is wherever you saved Apache ANT to. Also in "User variables",  
     add a variable called "Path" (if it's not already there). Add to it ";%ANT_HOME%/bin"
6.  Under "System variables" add ANDROID_NDK" and set the value to your ndk path. Then create a ANDROID_SDK system
	 variable and set the value to your sdk path. Also add ";" + you sdk path to the SYSTEM VARIABLE'S "Path"(Not the user variable "Path" made above)
7.  Download these repositories:<pre>
			`git clone git@github.com:zhuowei/aFileChooser.git`
 			`git clone git@github.com:zhuowei/BlockLauncher-app.git`
 			`git clone https://github.com/Chainfire/libsuperuser.git`</pre>
8.  You'll also be needing a MCPELauncher source folder with some changes that you'll be testing. I'll assume you're a
	 BlockLauncher developer and have this already.
9.  Extract all of these to the same directory. Personally I would put them all in a folder named "BLBUILD" in your 
     Desktop. I'm going to assume you did this.
10. Modify all folders in this way:
			Delete the "-master" in the names of:
				"MCPELauncher-app"
				"libsuperuser"
			Delete the "-prefragments" in the name of:
				"aFileChooser"
			Ensure the name of your BlockLauncher build is:
				"MCPELauncher"
11.  Edit your directories for each folder like this:
			MCPELauncher:
				<desktop>/BLBUILD/MCPELauncher/(files)
			MCPELauncher-app:
				<desktop>/BLBUILD/MCPELauncher-app/(files)
			libsuperuser:
				<desktop>/BLBUILD/libsuperuser/libsuperuser/(files)
			aFileChooser:
				<desktop>/BLBUILD/aFileChooser/aFileChooser/(files)
12.  Edit MCPELauncher/local.properties to point to your sdk
13.  Open your cmd prompt: Type in `cd <desktop>/BLBUILD/MCPELauncher`
	   Now type: `ndk-build` This should compile all of BlockLauncher's native code
14.  Type `cd <desktop>/BLBUILD/libsuperuser` then type `android update lib-project -p libsuperuser -t 1`
      Type `cd <desktop>/BLBUILD/aFileChooser` then type `android update lib-project -p aFileChooser -t 1`
      Type `cd <desktop>/BLBUILD` then type `android update project -p MCPELauncher-app -t 1`
      Type `cd <desktop>/BLBUILD` then type `android update project -p <desktop>\BLBUILD\MCPELauncher`
15.  Edit project.properties in this way:
			MCPELauncher: Ensure  target=10
			aFileChooser: Ensure  target=10
			libsuperuser: Ensure  target=10
			MCPELauncher-app: Ensure  target=17
16.  Type `cd <desktop>/BLBUILD/MCPELauncher-app` then type `ant clean debug`

17.  The build apk is MCPELauncher-app/bin/MCPELauncherApp-debug.apk

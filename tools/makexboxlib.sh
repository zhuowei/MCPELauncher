#!/bin/bash
set -e
rm -rf outlib || true
rm -rf outlib_interop || true
android create lib-project -t 7 -k com.microsoft.onlineid.sdk -p outlib
android create lib-project -t 7 -k com.microsoft.onlineid.interop -p outlib_interop
rm -rf outlib/res outlib/src outlib/AndroidManifest.xml
cat >outlib/AndroidManifest.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
      package="com.microsoft.onlineid.sdk"
      android:versionCode="1"
      android:versionName="1.0">
</manifest>
EOF
cat >outlib_interop/AndroidManifest.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
      package="com.microsoft.onlineid.interop"
      android:versionCode="1"
      android:versionName="1.0">
</manifest>
EOF
mkdir -p outlib/src/com/microsoft/onlineid/internal
cat >outlib/src/com/microsoft/onlineid/internal/Applications.java << EOF
package com.microsoft.onlineid.internal;
public class Applications {
	public static String buildClientAppUri(android.content.Context context, String second) {
		return "android-app://com.mojang.minecraftpe.H62DKCBHJP6WXXIV7RBFOGOL4NAK4E6Y";
	}
}
EOF
FILELIST=`find res -type f |grep -v /iconvr\.png|grep -v /icon\.png|grep -v /public.xml`
for i in $FILELIST
do
	mkdir -p outlib/`dirname $i`
	cp $i outlib/$i
done
JARDEL="org com/mojang com/amazon com/android com/googleplay com/microsoft/onlineid/sdk/R*.class com/microsoft/onlineid/interop/R*.class com/microsoft/onlineid/internal/Applications.class" # gson is needed
cp ../mcpe0150b1.apk ./mcpe.apk
dex2jar mcpe.apk
7z -tzip d mcpe_dex2jar.jar $JARDEL
mv mcpe_dex2jar.jar outlib/libs
rm -rf outlib_interop/res outlib_interop/src
mkdir outlib_interop/src
echo "android.library.reference.1=../outlib" >>outlib_interop/project.properties
cd outlib_interop
ant clean debug

CLASSPATH=bin/classes:libs/js.jar:/home/zhuowei/downloads/android-sdk-linux_86/platforms/android-10/android.jar
javah -classpath $CLASSPATH -o jni/nativepatch.h net.zhuoweizhang.pokerface.PokerFace
javah -classpath $CLASSPATH -o jni/modscript.h net.zhuoweizhang.mcpelauncher.ScriptManager

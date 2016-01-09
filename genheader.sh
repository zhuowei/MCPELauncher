CLASSPATH=bin/classes:libs/smalljs.jar:/home/zhuowei/downloads/android-sdk-linux_86/platforms/android-19/android.jar
javah -classpath $CLASSPATH -o jni/nativepatch.h net.zhuoweizhang.pokerface.PokerFace
javah -classpath $CLASSPATH -o jni/modscript.h net.zhuoweizhang.mcpelauncher.ScriptManager
javah -classpath $CLASSPATH -o jni/modscript_renderer_jni.h net.zhuoweizhang.mcpelauncher.api.modpe.RendererManager

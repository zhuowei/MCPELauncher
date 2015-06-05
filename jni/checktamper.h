static const char* checkTamperDec(const char* in, char* buf) {
	for (int i = 0; ; i++) {
		char c = in[i];
		buf[i] = c == 0? 0: c ^ 0x23;
		if (c == 0) break;
	}
	return (const char*) buf;
}

static int checkTamperCmp(const char* in, const char* decin) {
	char a, b;
	while((a = *in++) & (b = *decin++)) {
		if (a != (b ^ 0x23)) break;
	}
	return a - b;
}

static void checkTamper(JNIEnv* env, jobject activity) {
	// checks our package name
	// if not BlockLauncher, tampered
	jclass activityClass = (*env)->GetObjectClass(env, activity);
	char buf1[80];
	char buf2[80];
	
	jmethodID mid = (*env)->GetMethodID(env, activityClass, checkTamperDec("DFWsB@HBDFmBNF" /* getPackageName */, buf1),
		checkTamperDec("\x0b\noIBUB\x0cOBMD\x0cpWQJMD\x18" /* ()Ljava/lang/String; */, buf2));
	jstring packageNameString = (*env)->CallObjectMethod(env, activity, mid);
	const char* packageName = (*env)->GetStringUTFChars(env, packageNameString, NULL);
	if ((!checkTamperCmp(packageName, "MFW\rYKVLTFJYKBMD\rN@SFOBVM@KFQ" /* net.zhuoweizhang.mcpelauncher */)) ||
		!checkTamperCmp(packageName, "MFW\rYKVLTFJYKBMD\rN@SFOBVM@KFQ\rSQL" /* net.zhuoweizhang.mcpelauncher.pro */)) {
		bl_untampered = true;
	}
	(*env)->ReleaseStringUTFChars(env, packageNameString, packageName);
}

static bool bl_untampered;

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

static const char* checkTamperDec2(const char* in, char* buf) {
	for (int i = 0; ; i++) {
		char c = in[i];
		buf[i] = c == 0? 0: c - 40;
		if (c == 0) break;
	}
	return (const char*) buf;
}


static FILE* (*tamper_fopen)(const char *path, const char *mode);
static int (*tamper_fclose)(FILE*);

static void checkTamper(JNIEnv* env, jobject activity) {
	// checks our package name
	// if not BlockLauncher, tampered
	jclass activityClass = env->GetObjectClass(activity);
	char buf1[80];
	char buf2[80];

	/* BlockLauncher's shared UID is same as its package name */

	int (*getuid_)() = (int (*)()) dlsym(RTLD_DEFAULT, checkTamperDec("DFWVJG" /* getuid */ , buf1));
	tamper_fopen = (FILE* (*)(const char*, const char*)) dlsym(RTLD_DEFAULT, checkTamperDec("ELSFM" /* fopen */ , buf1));
	tamper_fclose = (int (*)(FILE*)) dlsym(RTLD_DEFAULT, checkTamperDec("E@OLPF" /* getuid */ , buf1));
	
	jmethodID mid = env->GetMethodID(activityClass, checkTamperDec("DFWsB@HBDFnBMBDFQ" /* getPackageManager */, buf1),
		checkTamperDec("\x0b\noBMGQLJG\x0c@LMWFMW\x0cSN\x0csB@HBDFnBMBDFQ\x18" /* ()Landroid/content/pm/PackageManager; */, buf2));
	jstring packageManager = (jstring) env->CallObjectMethod(activity, mid);
	jclass pmClass = (jclass) env->GetObjectClass(packageManager);
	mid = env->GetMethodID(pmClass, checkTamperDec("DFWmBNFeLQvJG" /* getNameForUid */, buf1),
		checkTamperDec("\x0bj\noIBUB\x0cOBMD\x0cpWQJMD\x18" /* (I)Ljava/lang/String; */, buf2));
	jstring packageNameString = (jstring) env->CallObjectMethod(packageManager, mid, getuid_());
	const char* packageName = env->GetStringUTFChars(packageNameString, NULL);
	if ((!checkTamperCmp(packageName, "MFW\rYKVLTFJYKBMD\rN@SFOBVM@KFQ" /* net.zhuoweizhang.mcpelauncher */)) ||
		!checkTamperCmp(packageName, "MFW\rYKVLTFJYKBMD\rN@SFOBVM@KFQ\rSQL" /* net.zhuoweizhang.mcpelauncher.pro */)) {
		bl_untampered = true;
	}
	env->ReleaseStringUTFChars(packageNameString, packageName);
}

static time_t tamper2time = 0;

static void checkTamper2() {
#if 0
	if (tamper2time < 1448427600) return;
	char buf1[80];
	const char proName[] = {0x57,0x8c,0x89,0x9c,0x89,0x57,0x8c,0x89,0x9c,0x89,0x57,0x96,0x8d,
		0x9c,0x56,0xa2,0x90,0x9d,0x97,0x9f,0x8d,0x91,0xa2,0x90,0x89,0x96,
		0x8f,0x56,0x95,0x8b,0x98,0x8d,0x94,0x89,0x9d,0x96,0x8b,0x90,0x8d,
		0x9a,0x56,0x98,0x9a,0x97,0x57,0x96,0x8d,0x9c,0x8e,0x8c,0x00};
	const char freeName[] = {0x57,0x8c,0x89,0x9c,0x89,0x57,0x8c,0x89,0x9c,0x89,0x57,0x96,0x8d,
		0x9c,0x56,0xa2,0x90,0x9d,0x97,0x9f,0x8d,0x91,0xa2,0x90,0x89,0x96,
		0x8f,0x56,0x95,0x8b,0x98,0x8d,0x94,0x89,0x9d,0x96,0x8b,0x90,0x8d,
		0x9a,0x57,0x96,0x8d,0x9c,0x8e,0x8c,0x00};
	//"/data/data/net.zhuoweizhang.mcpelauncher.pro/netfd"
	FILE* f = tamper_fopen(checkTamperDec2(proName, buf1), "w");
	if (f) {
		tamper_fclose(f);
		return;
	}
	//"/data/data/net.zhuoweizhang.mcpelauncher/netfd"
	f = tamper_fopen(checkTamperDec2(freeName, buf1), "w");
	if (f) {
		tamper_fclose(f);
		return;
	}
	*((void**) &bl_JavaVM) = (void*) &checkTamper2;
#endif
}

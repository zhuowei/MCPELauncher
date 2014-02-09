package net.zhuoweizhang.mcpelauncher;

import java.io.*;

import dalvik.system.DexClassLoader;

import org.mozilla.javascript.*;

public class ScriptTranslationCache {

	public static final String SCRIPT_DEX_DIR = "dex";
	public static final String SCRIPT_ODEX_DIR = "odex";
	public static final String CLASS_PACKAGE = "modpe.";

	public static Script get(android.content.Context context, File file) throws IOException {
		//does the cached file exist?
		File dexFile = getDexFile(context, file);
		if (!dexFile.exists() || dexFile.lastModified() < file.lastModified()) {
			dexScript(context, file);
		}
		//load the dex
		File odexDir = context.getDir(SCRIPT_ODEX_DIR, 0);
		DexClassLoader classLoader = new DexClassLoader(dexFile.getAbsolutePath(), odexDir.getAbsolutePath(),
			null, ScriptTranslationCache.class.getClassLoader());
		try {
			Class<? extends Script> scriptClass = (Class<? extends Script>) classLoader.loadClass(CLASS_PACKAGE + getClassName(file));
			Script script = scriptClass.newInstance();
			return script;
		} catch (ClassNotFoundException cnf) {
			throw new RuntimeException(cnf);
		} catch (InstantiationException in) {
			throw new RuntimeException(in);
		} catch (IllegalAccessException in) {
			//impossible; the constructor should not be private
			throw new RuntimeException(in);
		}
	}

	private static String getClassName(File f) {
		String name = f.getName();
		String nojs = name.substring(0, name.length() - 3);
		String mainClassName = getClassName(nojs);
		return mainClassName;
	}

	/**
	 * Verify that class file names are legal Java identifiers.  Substitute
	 * illegal characters with underscores, and prepend the name with an
	 * underscore if the file name does not begin with a JavaLetter.
	 */

	private static String getClassName(String name) {
		char[] s = new char[name.length()+1];
		char c;
		int j = 0;

		if (!Character.isJavaIdentifierStart(name.charAt(0))) {
			s[j++] = '_';
		}
		for (int i=0; i < name.length(); i++, j++) {
			c = name.charAt(i);
			if ( Character.isJavaIdentifierPart(c) ) {
				s[j] = c;
			} else {
				s[j] = '_';
			}
		}
		return (new String(s)).trim();
	 }

	private static File getDexFile(android.content.Context context, File file) {
		File dexFileDir = context.getDir(SCRIPT_DEX_DIR, 0);
		return new File(dexFileDir, file.getName() + ".dex");
	}

	private static void dexScript(android.content.Context context, File file) {
		TranslateThread parseRunner = new TranslateThread(context, file);
		Thread t = new Thread(Thread.currentThread().getThreadGroup(), parseRunner, 
			"BlockLauncher parse thread", 256*1024);
		t.start();
		try {
			t.join(); //block on this thread
		} catch (InterruptedException ie) {
			//shouldn't happen
		}
		if (parseRunner.error != null) {
			RuntimeException back;
			if (parseRunner.error instanceof RuntimeException) {
				back = (RuntimeException) parseRunner.error;
			} else {
				back = new RuntimeException(parseRunner.error);
			}
			throw back; //Thursdays
		}
	}

	private static void dexScriptImpl(android.content.Context context, File file) throws IOException {
	}

	private static class TranslateThread implements Runnable {
		private android.content.Context context;
		private File file;
		public Exception error;
		public TranslateThread(android.content.Context context, File file) {
			this.context = context;
			this.file = file;
		}
		public void run() {
			try {
				dexScriptImpl(context, file);
			} catch (Exception e) {
				e.printStackTrace();
				error = e;
			}
		}
	}
}
		

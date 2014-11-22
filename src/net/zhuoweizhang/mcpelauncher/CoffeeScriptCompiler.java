package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class CoffeeScriptCompiler {

	public static boolean isCoffeeScript(File input) {
		return input.getName().toLowerCase().endsWith(".coffee") || isLiterateCoffeeScript(input);
	}

	public static boolean isLiterateCoffeeScript(File input) {
		return input.getName().toLowerCase().endsWith(".litcoffee");
	}

	public static String outputName(String input) {
		return input.substring(0, input.lastIndexOf(".")) + ".js";
	}

	public static void compileFile(File input, File output) throws IOException {
		//read the file
		//call compile with the string
		//write the string to the new file
		InputStream is = new FileInputStream(input);
		int length = (int) input.length();
		byte[] data = new byte[length];
		is.read(data);
		is.close();
		Charset utf8 = Charset.forName("UTF-8");
		String inputString = new String(data, utf8);
		String outputString = compile(inputString, isLiterateCoffeeScript(input));
		OutputStream os = new FileOutputStream(output);
		os.write(outputString.getBytes(utf8));
		os.close();
	}

	public static String compile(String input, boolean literate) {
		System.gc();
		TranslateThread parseRunner = new TranslateThread(input, literate);
		Thread t = new Thread(Thread.currentThread().getThreadGroup(), parseRunner, 
			"BlockLauncher parse thread", 256*1024);
		t.start();
		try {
			t.join(); //block on this thread
		} catch (InterruptedException ie) {
			//shouldn't happen
		}
		System.gc();
		if (parseRunner.error != null) {
			RuntimeException back;
			if (parseRunner.error instanceof RuntimeException) {
				back = (RuntimeException) parseRunner.error;
			} else {
				back = new RuntimeException(parseRunner.error);
			}
			throw back; //Thursdays
		}
		return parseRunner.output;
	}

	private static class TranslateThread implements Runnable {
		public boolean literate;
		public String input;
		public String output;
		public Throwable error;
		public TranslateThread(String input, boolean literate) {
			this.input = input;
			this.literate = literate;
		}
		public void run() {
			try {
				output = compileForReal(input, literate);
			} catch (Exception e) {
				error = e;
			}
		}
	}
	private static String compileForReal(String input, boolean literate) {
		//set up Rhino
		//load coffee-script.js from assets and execute it
		//grab the compile function
		//invoke it with our parameters (bare, literate if appropriate, no header)
		//return the compiled string
		/*
		Context ctx = Context.enter();
		Script compilerScript = new org.coffeescript.CoffeeScript();
		Scriptable scope = ctx.initStandardObjects();
		compilerScript.exec(ctx, scope);
		//at this point, the CoffeeScript compiler has been initialized in the Rhino context.

		Scriptable compilerParams = ctx.newObject(scope);
		ScriptableObject.putProperty(compilerParams, "bare", true); //bare mode
		ScriptableObject.putProperty(compilerParams, "literate", literate); //bare mode

		Scriptable coffeeScript = (Scriptable) ScriptableObject.getProperty(scope, "CoffeeScript");
		Function coffeeScriptCompile = (Function) ScriptableObject.getProperty(coffeeScript, "compile");

		String output = (String) coffeeScriptCompile.call(ctx, scope, scope, new Object[] {input, compilerParams});
		Context.exit();
		return output;
		*/
		throw new RuntimeException("CoffeeScript compiler has been removed");
	}
}
